package com.baru.backend.dto.flight;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FlightOfferDto {
    private String id;

    private String airlineCode;     // 항공사 코드(예: KE, TW)
    private String flightNo;        // 예: TW207

    private String priceTotal;      // "140.40" 같은 문자열로 내려옴(Amadeus 원본)
    private String currency;        // KRW 등

    private String departureAirport; // ICN
    private String arrivalAirport;   // NRT
    private String departureAt;      // 2026-01-10T06:05:00
    private String arrivalAt;        // 2026-01-10T07:35:00

    private int stops;               // 경유 횟수
    private List<SegmentDto> segments;

    @Getter
    @Builder
    public static class SegmentDto {
        private String carrierCode;
        private String flightNumber;
        private String from;
        private String to;
        private String departureAt;
        private String arrivalAt;
    }
}

