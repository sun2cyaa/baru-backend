package com.baru.backend.service.amadeus;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class AmadeusFlightService {

    private final WebClient webClient;
    private final AmadeusAuthService auth;

    public AmadeusFlightService(
            @Value("${amadeus.base-url}") String baseUrl,
            AmadeusAuthService auth
    ) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.auth = auth;
    }

    // (옵션) 기존 편도 호출 유지용 오버로드
    public Map<String, Object> searchOffers(
            String origin,
            String destination,
            String departureDate,
            int adults,
            int max,
            String currencyCode
    ) {
        return searchOffers(origin, destination, departureDate, null, adults, max, currencyCode);
    }

    // 왕복(returnDate) 지원
    @SuppressWarnings("unchecked")
    public Map<String, Object> searchOffers(
            String origin,
            String destination,
            String departureDate,
            String returnDate,
            int adults,
            int max,
            String currencyCode
    ) {
        String token = auth.getAccessToken();

        return (Map<String, Object>) webClient.get()
                .uri(uriBuilder -> {
                    var b = uriBuilder
                            .path("/v2/shopping/flight-offers")
                            .queryParam("originLocationCode", origin)
                            .queryParam("destinationLocationCode", destination)
                            .queryParam("departureDate", departureDate)
                            .queryParam("adults", adults)
                            .queryParam("max", max)
                            .queryParam("currencyCode", currencyCode);

                    if (returnDate != null && !returnDate.isBlank()) {
                        b = b.queryParam("returnDate", returnDate);
                    }
                    return b.build();
                })
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }
}
