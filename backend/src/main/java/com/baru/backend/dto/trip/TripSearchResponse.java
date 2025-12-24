package com.baru.backend.dto.trip;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class TripSearchResponse {

    private TripSearchRequest requested;
    private Exchange exchange;
    private Budget budget;
    private List<FlightCard> flights;
    private List<Object> hotels; // 나중에 Hotel DTO로 교체

    @Getter
    @Builder
    public static class Exchange {
        private String base;
        private Rates rates;
        private String updatedAt;
    }

    @Getter
    @Builder
    public static class Rates {
        private Double JPY;
        private Double USD;
    }

    @Getter
    @Builder
    public static class Budget {
        private int budgetWon;
        private int estimatedTotalWon;
        private int remainingWon;
    }

    @Getter
    @Builder
    public static class FlightCard {
        private String airline;
        private int priceWon;
        private String departureAirport;
        private String departureTime;
        private String arrivalAirport;
        private String arrivalTime;
        private int durationMinutes;
        private int stops;
        private List<Segment> segments;
    }

    public record Segment(
            String flightNo,
            String from,
            String to,
            String depTime,
            String arrTime
    ) {}
}

