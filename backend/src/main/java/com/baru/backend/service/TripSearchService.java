package com.baru.backend.service;

import com.baru.backend.dto.trip.TripSearchRequest;
import com.baru.backend.dto.trip.TripSearchResponse;
import com.baru.backend.service.amadeus.AmadeusFlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TripSearchService {

    private final AmadeusFlightService amadeusFlightService;
    private final ExchangeRateService exchangeRateService;

    @SuppressWarnings("unchecked")
    public TripSearchResponse search(TripSearchRequest req) {

        /* 1️⃣ 환율 (base = KRW) */
        Map<String, Double> ratesMap = exchangeRateService.latestRates("KRW");
        Double jpy = ratesMap.get("JPY");
        Double usd = ratesMap.get("USD");

        /* 2️⃣ 출/도착 공항 */
        String origin = "ICN";
        String destination = "JAPAN".equalsIgnoreCase(req.preferredRegion())
                ? "NRT"
                : "NRT";

        /* 3️⃣ Amadeus 항공권 조회 (Map으로 받음) */
        Map<String, Object> root = amadeusFlightService.searchOffers(
                origin,
                destination,
                req.departDate(),
                req.returnDate(),
                req.people(),
                10,
                "KRW"
        );

        /* 4️⃣ data 파싱 */
        List<Map<String, Object>> data =
                (List<Map<String, Object>>) root.getOrDefault("data", List.of());

        List<TripSearchResponse.FlightCard> flights = new ArrayList<>();

        for (Map<String, Object> offer : data) {

            /* price */
            Map<String, Object> price = (Map<String, Object>) offer.get("price");
            String currency = price.getOrDefault("currency", "KRW").toString();
            double total = Double.parseDouble(price.get("total").toString());
            int priceWon = toKrw(currency, total, ratesMap);

            /* itineraries[0] */
            List<Map<String, Object>> itineraries =
                    (List<Map<String, Object>>) offer.get("itineraries");
            if (itineraries == null || itineraries.isEmpty()) continue;

            Map<String, Object> itin0 = itineraries.get(0);
            List<Map<String, Object>> segments =
                    (List<Map<String, Object>>) itin0.get("segments");
            if (segments == null || segments.isEmpty()) continue;

            Map<String, Object> first = segments.get(0);
            Map<String, Object> last = segments.get(segments.size() - 1);

            Map<String, Object> dep = (Map<String, Object>) first.get("departure");
            Map<String, Object> arr = (Map<String, Object>) last.get("arrival");

            String depAirport = dep.get("iataCode").toString();
            String depTime = dep.get("at").toString();
            String arrAirport = arr.get("iataCode").toString();
            String arrTime = arr.get("at").toString();

            int stops = Math.max(0, segments.size() - 1);

            /* duration */
            String durationIso = itin0.get("duration").toString();
            int durationMinutes = (int) Duration.parse(durationIso).toMinutes();

            /* segments DTO */
            List<TripSearchResponse.Segment> segmentDtos = new ArrayList<>();
            for (Map<String, Object> s : segments) {
                String flightNo =
                        s.get("carrierCode").toString() + s.get("number").toString();

                Map<String, Object> sDep = (Map<String, Object>) s.get("departure");
                Map<String, Object> sArr = (Map<String, Object>) s.get("arrival");

                segmentDtos.add(new TripSearchResponse.Segment(
                        flightNo,
                        sDep.get("iataCode").toString(),
                        sArr.get("iataCode").toString(),
                        sDep.get("at").toString(),
                        sArr.get("at").toString()
                ));
            }

            flights.add(TripSearchResponse.FlightCard.builder()
                    .airline(first.get("carrierCode").toString())
                    .priceWon(priceWon)
                    .departureAirport(depAirport)
                    .departureTime(depTime)
                    .arrivalAirport(arrAirport)
                    .arrivalTime(arrTime)
                    .durationMinutes(durationMinutes)
                    .stops(stops)
                    .segments(segmentDtos)
                    .build());
        }

        /* 5️⃣ 예산 계산 */
        int minFlightWon = flights.stream()
                .mapToInt(TripSearchResponse.FlightCard::getPriceWon)
                .min()
                .orElse(0);

        int remainingWon = req.budgetWon() - minFlightWon;

        /* 6️⃣ 응답 */
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
                        .budgetWon(req.budgetWon())
                        .estimatedTotalWon(minFlightWon)
                        .remainingWon(remainingWon)
                        .build())
                .flights(flights)
                .hotels(List.of())
                .build();
    }

    /* 통화 → KRW 변환 */
    private int toKrw(String currency, double amount, Map<String, Double> ratesMap) {
        if ("KRW".equalsIgnoreCase(currency)) {
            return (int) Math.round(amount);
        }
        Double rate = ratesMap.get(currency.toUpperCase());
        if (rate == null || rate == 0) {
            throw new IllegalStateException("환율 없음: " + currency);
        }
        return (int) Math.round(amount / rate);
    }
}
