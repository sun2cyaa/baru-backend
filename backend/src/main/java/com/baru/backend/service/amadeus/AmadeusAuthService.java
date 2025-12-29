package com.baru.backend.service.amadeus;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
public class AmadeusAuthService {

    private final WebClient webClient;

    @Value("${amadeus.client-id}")
    private String clientId;

    @Value("${amadeus.client-secret}")
    private String clientSecret;

    public AmadeusAuthService(@Value("${amadeus.base-url}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @SuppressWarnings("unchecked")
    public String getAccessToken() {
        try {
            Map<String, Object> response = webClient.post()
                    .uri("/v1/security/oauth2/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(
                            "grant_type=client_credentials" +
                                    "&client_id=" + clientId +
                                    "&client_secret=" + clientSecret
                    )
                    .retrieve()
                    .bodyToMono(Map.class)   // ✅ Map으로 받기
                    .block();

            if (response == null || !response.containsKey("access_token")) {
                throw new IllegalStateException("Amadeus token response invalid");
            }

            return response.get("access_token").toString();

        } catch (WebClientResponseException e) {
            throw new IllegalStateException(
                    "Amadeus Auth failed: " + e.getStatusCode() + " " + e.getResponseBodyAsString(),
                    e
            );
        }
    }
}
