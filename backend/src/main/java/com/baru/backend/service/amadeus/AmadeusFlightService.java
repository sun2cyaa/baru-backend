package com.baru.backend.service.amadeus;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@Service
public class AmadeusFlightService {

    private final WebClient webClient;
    private final AmadeusAuthService auth;

    public AmadeusFlightService(
            WebClient.Builder builder,
            @Value("${amadeus.base-url}") String baseUrl,
            AmadeusAuthService auth
    ) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.auth = auth;
    }

    /** Airport Routes API: 출발 공항 기준 직항 목적지 목록 */
    public Map<String, Object> directDestinations(String departureAirportCode, Integer max) {
        String token = auth.getAccessToken();

        try {
            return webClient.get()
                    .uri(uriBuilder -> {
                        var b = uriBuilder
                                .path("/v1/airport/direct-destinations")
                                .queryParam("departureAirportCode", departureAirportCode);
                        if (max != null && max > 0) b = b.queryParam("max", max);
                        return b.build();
                    })
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            throw new IllegalStateException("Amadeus direct-destinations 실패: "
                    + e.getStatusCode() + " / " + e.getResponseBodyAsString(), e);
        }
    }

    /** 편도 항공권 조회 (returnDate 없이 호출) */
    public Map<String, Object> searchOffersOneWay(
            String origin,
            String destination,
            String departureDate,
            int adults,
            int max,
            String currencyCode
    ) {
        String token = auth.getAccessToken();

        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/shopping/flight-offers")
                            .queryParam("originLocationCode", origin)
                            .queryParam("destinationLocationCode", destination)
                            .queryParam("departureDate", departureDate)
                            .queryParam("adults", adults)
                            .queryParam("max", max)
                            .queryParam("currencyCode", currencyCode)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            throw new IllegalStateException("Amadeus flight-offers(ONEWAY) 실패: "
                    + e.getStatusCode() + " / " + e.getResponseBodyAsString(), e);
        }
    }

    /** 왕복(같은 공항으로 귀국) — 필요하면 사용 */
    public Map<String, Object> searchOffersRoundTrip(
            String origin,
            String destination,
            String departureDate,
            String returnDate,
            int adults,
            int max,
            String currencyCode
    ) {
        String token = auth.getAccessToken();

        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/shopping/flight-offers")
                            .queryParam("originLocationCode", origin)
                            .queryParam("destinationLocationCode", destination)
                            .queryParam("departureDate", departureDate)
                            .queryParam("returnDate", returnDate)
                            .queryParam("adults", adults)
                            .queryParam("max", max)
                            .queryParam("currencyCode", currencyCode)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            throw new IllegalStateException("Amadeus flight-offers(ROUNDTRIP) 실패: "
                    + e.getStatusCode() + " / " + e.getResponseBodyAsString(), e);
        }
    }
}
