package com.baru.backend.dto.trip;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TripSearchResponse {

    private TripSearchRequest requested;
    private Exchange exchange;
    private Budget budget;
    private List<FlightCard> flights;
    private List<HotelCard> hotels;

    @Getter
    @Builder
    public static class Exchange {
        private String base;     // "KRW"
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
        private String airline;          // "KE"
        private int priceWon;           // 왕복(편도2개 합산) 총액
        private String departureAirport; // 출발공항
        private String departureTime;    // 출발시간(OUT 기준)
        private String arrivalAirport;   // 도착공항(추천 목적지)
        private String arrivalTime;      // 도착시간(OUT 기준)
        private int durationMinutes;     // OUT 기준
        private int stops;               // OUT 기준
        private List<Segment> segments;  // OUT 기준(필요하면 IN도 확장 가능)
    }

    @Getter
    @Builder
    public static class Segment {
        private String flightNo;
        private String from;
        private String to;
        private String depTime;
        private String arrTime;
    }

    @Getter
    @Builder
    public static class HotelCard {
        private String hotelId;
        private String name;
        private String cityCode;

        private int totalWon;     // 체류기간 총액
        private int perNightWon;  // 1박당
        private int nights;
    }
}
