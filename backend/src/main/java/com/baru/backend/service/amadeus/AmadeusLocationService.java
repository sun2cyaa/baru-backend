package com.baru.backend.service.amadeus;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Slf4j
@Service
public class AmadeusLocationService {

    private final WebClient webClient;
    private final AmadeusAuthService auth;

    public AmadeusLocationService(
            WebClient.Builder builder,
            @Value("${amadeus.base-url}") String baseUrl,
            AmadeusAuthService auth
    ) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.auth = auth;
    }

    public String resolveCityCode(String destinationIata) {
        if (destinationIata == null || destinationIata.isBlank()) return null;

        String keyword = destinationIata.trim().toUpperCase(Locale.ROOT);
        String token = auth.getAccessToken();

        try {
            JsonNode root = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/reference-data/locations")
                            .queryParam("subType", "AIRPORT,CITY")
                            .queryParam("keyword", keyword)
                            .queryParam("page[limit]", 1)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            JsonNode data = root == null ? null : root.path("data");
            if (data == null || !data.isArray() || data.isEmpty()) return null;

            JsonNode first = data.get(0);

            String cityCode = first.path("address").path("cityCode").asText(null);
            if (cityCode != null && !cityCode.isBlank()) return cityCode.trim().toUpperCase(Locale.ROOT);

            String iata = first.path("iataCode").asText(null);
            if (iata != null && !iata.isBlank()) return iata.trim().toUpperCase(Locale.ROOT);

            return null;

        } catch (WebClientResponseException e) {
            log.error("[AMADEUS] locations error status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Amadeus locations failed: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("[AMADEUS] locations unknown error", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Amadeus locations failed");
        }
    }
}
