package com.baru.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class ExchangeRateService {

    private final RestClient restClient = RestClient.create();

    @Value("${exchange.api.base}")
    private String baseUrl;

    @Value("${exchange.api.key}")
    private String apiKey;

    /**
     * baseCurrency 예: "KRW"
     * return: {"JPY":0.11,"USD":0.00076,...}
     */
    @SuppressWarnings("unchecked")
    public Map<String, Double> latestRates(String baseCurrency) {

        String url = baseUrl + "/" + apiKey + "/latest/" + baseCurrency;

        Map<String, Object> res = restClient.get()
                .uri(url)
                .retrieve()
                .body(Map.class);   // ✅ Map으로 받기

        if (res == null || !res.containsKey("conversion_rates")) {
            throw new IllegalStateException("ExchangeRate API response invalid");
        }

        return (Map<String, Double>) res.get("conversion_rates");
    }
}
