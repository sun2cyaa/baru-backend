package com.baru.backend.service.amadeus;

import com.baru.backend.dto.trip.TripSearchResponse;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
public class AmadeusHotelService {

    private final WebClient webClient;
    private final AmadeusAuthService auth;

    public AmadeusHotelService(
            WebClient.Builder builder,
            @Value("${amadeus.base-url}") String baseUrl,
            AmadeusAuthService auth
    ) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.auth = auth;
    }

    public List<HotelMeta> listHotelsByCity(String cityCode, int limit) {
        if (cityCode == null || cityCode.isBlank()) return List.of();
        String token = auth.getAccessToken();

        try {
            JsonNode root = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/reference-data/locations/hotels/by-city")
                            .queryParam("cityCode", cityCode)
                            .queryParam("radius", 20)
                            .queryParam("radiusUnit", "KM")
                            .queryParam("hotelSource", "ALL")
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            JsonNode data = root == null ? null : root.path("data");
            if (data == null || !data.isArray()) return List.of();

            List<HotelMeta> hotels = new ArrayList<>();
            for (JsonNode h : data) {
                String id = h.path("hotelId").asText(null);
                String name = h.path("name").asText("");
                if (id == null || id.isBlank()) continue;
                hotels.add(new HotelMeta(id, name));
                if (hotels.size() >= limit) break;
            }
            return hotels;

        } catch (WebClientResponseException e) {
            log.error("[AMADEUS] hotels/by-city error status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Amadeus hotels-by-city failed: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("[AMADEUS] hotels/by-city unknown error", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Amadeus hotels-by-city failed");
        }
    }

    public TripSearchResponse.HotelCard findCheapestHotelOffer(
            String cityCode,
            List<HotelMeta> hotelMetas,
            String checkInDate,
            String checkOutDate,
            int adults
    ) {
        if (hotelMetas == null || hotelMetas.isEmpty()) return null;

        int nights = calcNights(checkInDate, checkOutDate);
        if (nights <= 0) return null;

        String hotelIds = hotelMetas.stream()
                .map(HotelMeta::hotelId)
                .limit(20)
                .reduce((a, b) -> a + "," + b)
                .orElse(null);

        if (hotelIds == null) return null;

        String token = auth.getAccessToken();

        JsonNode root;
        try {
            root = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v3/shopping/hotel-offers")
                            .queryParam("hotelIds", hotelIds)
                            .queryParam("adults", adults)
                            .queryParam("checkInDate", checkInDate)
                            .queryParam("checkOutDate", checkOutDate)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("[AMADEUS] hotel-offers error status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            // 호텔이 막히는 경우가 많아서: 여기서는 null 반환으로 후보만 떨어뜨리게 해도 됨
            return null;
        } catch (Exception e) {
            log.error("[AMADEUS] hotel-offers unknown error", e);
            return null;
        }

        JsonNode data = root == null ? null : root.path("data");
        if (data == null || !data.isArray()) return null;

        TripSearchResponse.HotelCard best = null;
        int bestTotal = Integer.MAX_VALUE;

        for (JsonNode hotel : data) {
            String hotelId = hotel.path("hotel").path("hotelId").asText(null);
            String name = hotel.path("hotel").path("name").asText("");

            JsonNode offers = hotel.path("offers");
            if (!offers.isArray() || offers.isEmpty()) continue;

            for (JsonNode offer : offers) {
                String totalStr = offer.path("price").path("total").asText(null);
                if (totalStr == null || totalStr.isBlank()) continue;

                int totalWon = parseMoney(totalStr);
                if (totalWon <= 0) continue;

                if (totalWon < bestTotal) {
                    bestTotal = totalWon;
                    int perNight = Math.max(1, totalWon / nights);

                    best = TripSearchResponse.HotelCard.builder()
                            .hotelId(hotelId)
                            .name(name)
                            .cityCode(cityCode)
                            .totalWon(totalWon)
                            .perNightWon(perNight)
                            .nights(nights)
                            .build();
                }
            }
        }

        return best;
    }

    private int calcNights(String checkIn, String checkOut) {
        try {
            LocalDate in = LocalDate.parse(checkIn);
            LocalDate out = LocalDate.parse(checkOut);
            return (int) ChronoUnit.DAYS.between(in, out);
        } catch (Exception e) {
            return 0;
        }
    }

    private int parseMoney(String s) {
        try {
            double d = Double.parseDouble(s);
            return (int) Math.round(d);
        } catch (Exception e) {
            return 0;
        }
    }

    public record HotelMeta(String hotelId, String name) {}
}
