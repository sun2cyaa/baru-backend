package com.baru.backend.service;

import com.baru.backend.dto.trip.TripSearchRequest;
import com.baru.backend.dto.trip.TripSearchResponse;
import com.baru.backend.service.amadeus.AmadeusFlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripSearchService {

    private final AmadeusFlightService amadeusFlightService;
    private final ExchangeRateService exchangeRateService;

    @SuppressWarnings("unchecked")
    public TripSearchResponse search(TripSearchRequest req) {

        // 1) 환율 (base=KRW)
        Map<String, Double> ratesMap = exchangeRateService.latestRates("KRW");
        Double jpy = ratesMap.get("JPY");
        Double usd = ratesMap.get("USD");

        // 2) 입력값 정리(프론트가 안 보내면 기본값)
        String departDate = req.getDepartDate();
        String returnDate = req.getReturnDate();
        int adults = Math.max(1, req.getPeople());

        String origin = (req.getOriginLocationCode() == null || req.getOriginLocationCode().isBlank())
                ? "ICN"
                : req.getOriginLocationCode().trim().toUpperCase();

        // 귀국공항(요구사항: 출발과 다를 수 있음)
        String homeReturn = (req.getDestinationLocationCode() == null || req.getDestinationLocationCode().isBlank())
                ? origin
                : req.getDestinationLocationCode().trim().toUpperCase();

        boolean isDomestic = Boolean.TRUE.equals(req.getDomestic()); // null이면 false(국외)로 처리

        // 3) 목적지 후보 뽑기 (Airport Routes API)
        Map<String, Object> routesRoot = amadeusFlightService.directDestinations(origin, 80);
        List<Map<String, Object>> routeData =
                (List<Map<String, Object>>) routesRoot.getOrDefault("data", List.of());

        // 후보 iataCode + countryCode 추출
        List<Dest> candidates = new ArrayList<>();
        for (Map<String, Object> item : routeData) {
            Object iataObj = item.get("iataCode");
            if (iataObj == null) continue;
            String destIata = iataObj.toString().toUpperCase();

            String countryCode = null;
            Object addrObj = item.get("address");
            if (addrObj instanceof Map<?, ?> addrMap) {
                Object cc = ((Map<String, Object>) addrMap).get("countryCode");
                if (cc != null) countryCode = cc.toString().toUpperCase();
            }

            // 국내/국외 필터
            if (isDomestic) {
                if (!"KR".equals(countryCode)) continue;
            } else {
                if ("KR".equals(countryCode)) continue;
            }

            // 출발공항과 동일한 목적지는 제외
            if (origin.equals(destIata)) continue;

            candidates.add(new Dest(destIata, countryCode));
        }

        // 너무 많으면 호출량 폭발하니까 상한
        candidates = candidates.stream().limit(20).collect(Collectors.toList());

        // 4) 각 목적지별로 OUT + IN 최저가 합산해서 FlightCard 생성
        List<TripSearchResponse.FlightCard> flights = new ArrayList<>();

        for (Dest d : candidates) {
            String destination = d.iata;

            // OUT: origin -> destination
            Map<String, Object> outRoot = amadeusFlightService.searchOffersOneWay(
                    origin, destination, departDate, adults, 1, "KRW"
            );

            // IN: destination -> homeReturn
            Map<String, Object> inRoot = amadeusFlightService.searchOffersOneWay(
                    destination, homeReturn, returnDate, adults, 1, "KRW"
            );

            Map<String, Object> outOffer = firstOffer(outRoot);
            Map<String, Object> inOffer = firstOffer(inRoot);
            if (outOffer == null || inOffer == null) continue;

            int outWon = offerTotalWon(outOffer, ratesMap);
            int inWon = offerTotalWon(inOffer, ratesMap);
            int totalWon = outWon + inWon;

            // 예산 컷(원하면 비율로 조정 가능)
            if (req.getBudgetWon() > 0 && totalWon > req.getBudgetWon()) {
                continue;
            }

            // 화면 표시용: OUT 편도 기준 정보(시간/구간)
            ItinInfo outItin = parseItinerary0(outOffer);

            flights.add(TripSearchResponse.FlightCard.builder()
                    .airline(outItin.airline)
                    .priceWon(totalWon) // ✅ 왕복 총액
                    .departureAirport(outItin.depAirport)
                    .departureTime(outItin.depTime)
                    .arrivalAirport(outItin.arrAirport)  // ✅ 추천 목적지
                    .arrivalTime(outItin.arrTime)
                    .durationMinutes(outItin.durationMinutes)
                    .stops(outItin.stops)
                    .segments(outItin.segments)
                    .build());

            if (flights.size() >= 10) break; // 상위 10개만
        }

        // 가격 낮은 순 정렬
        flights.sort(Comparator.comparingInt(TripSearchResponse.FlightCard::getPriceWon));

        // 5) 예산 계산(최저 항공권 기준)
        int minFlightWon = flights.stream()
                .mapToInt(TripSearchResponse.FlightCard::getPriceWon)
                .min()
                .orElse(0);

        int remainingWon = req.getBudgetWon() - minFlightWon;

        // 6) 응답
        return TripSearchResponse.builder()
                .requested(req)
                .exchange(TripSearchResponse.Exchange.builder()
                        .base("KRW")
                        .rates(TripSearchResponse.Rates.builder()
                                .JPY(jpy)
                                .USD(usd)
                                .build())
                        .updatedAt(LocalDateTime.now().toString())
                        .build())
                .budget(TripSearchResponse.Budget.builder()
                        .budgetWon(req.getBudgetWon())
                        .estimatedTotalWon(minFlightWon)
                        .remainingWon(remainingWon)
                        .build())
                .flights(flights)
                .hotels(List.of())
                .build();
    }

    // ===== helpers =====

    @SuppressWarnings("unchecked")
    private Map<String, Object> firstOffer(Map<String, Object> root) {
        List<Map<String, Object>> data = (List<Map<String, Object>>) root.getOrDefault("data", List.of());
        if (data.isEmpty()) return null;
        return data.get(0);
    }

    @SuppressWarnings("unchecked")
    private int offerTotalWon(Map<String, Object> offer, Map<String, Double> ratesMap) {
        Map<String, Object> price = (Map<String, Object>) offer.get("price");
        if (price == null) return 0;

        String currency = price.getOrDefault("currency", "KRW").toString();
        double total = Double.parseDouble(price.get("total").toString());
        return toKrw(currency, total, ratesMap);
    }

    @SuppressWarnings("unchecked")
    private ItinInfo parseItinerary0(Map<String, Object> offer) {
        List<Map<String, Object>> itineraries = (List<Map<String, Object>>) offer.get("itineraries");
        Map<String, Object> itin0 = itineraries.get(0);

        List<Map<String, Object>> segments = (List<Map<String, Object>>) itin0.get("segments");
        Map<String, Object> first = segments.get(0);
        Map<String, Object> last = segments.get(segments.size() - 1);

        Map<String, Object> dep = (Map<String, Object>) first.get("departure");
        Map<String, Object> arr = (Map<String, Object>) last.get("arrival");

        String depAirport = dep.get("iataCode").toString();
        String depTime = dep.get("at").toString();
        String arrAirport = arr.get("iataCode").toString();
        String arrTime = arr.get("at").toString();

        int stops = Math.max(0, segments.size() - 1);
        int durationMinutes = (int) Duration.parse(itin0.get("duration").toString()).toMinutes();

        String airline = first.get("carrierCode").toString();

        List<TripSearchResponse.Segment> segDtos = new ArrayList<>();
        for (Map<String, Object> s : segments) {
            String flightNo = s.get("carrierCode").toString() + s.get("number").toString();
            Map<String, Object> sDep = (Map<String, Object>) s.get("departure");
            Map<String, Object> sArr = (Map<String, Object>) s.get("arrival");

            segDtos.add(TripSearchResponse.Segment.builder()
                    .flightNo(flightNo)
                    .from(sDep.get("iataCode").toString())
                    .to(sArr.get("iataCode").toString())
                    .depTime(sDep.get("at").toString())
                    .arrTime(sArr.get("at").toString())
                    .build());
        }

        return new ItinInfo(airline, depAirport, depTime, arrAirport, arrTime, durationMinutes, stops, segDtos);
    }

    private int toKrw(String currency, double amount, Map<String, Double> ratesMap) {
        if ("KRW".equalsIgnoreCase(currency)) return (int) Math.round(amount);

        Double rate = ratesMap.get(currency.toUpperCase());
        if (rate == null || rate == 0) {
            throw new IllegalStateException("환율 없음: " + currency);
        }
        // (너희 ExchangeRateService가 KRW base일 때) amount / rate 로 KRW 환산하던 기존 로직 유지 :contentReference[oaicite:6]{index=6}
        return (int) Math.round(amount / rate);
    }

    private record Dest(String iata, String countryCode) {}
    private record ItinInfo(
            String airline,
            String depAirport, String depTime,
            String arrAirport, String arrTime,
            int durationMinutes,
            int stops,
            List<TripSearchResponse.Segment> segments
    ) {}
}
