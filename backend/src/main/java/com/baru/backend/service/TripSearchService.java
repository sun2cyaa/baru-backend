package com.baru.backend.service;

import com.baru.backend.dto.trip.TripSearchRequest;
import com.baru.backend.dto.trip.TripSearchResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TripSearchService {

    public TripSearchResponse search(TripSearchRequest req) {
        // ✅ 프론트 연결용 mock 데이터
        TripSearchResponse.FlightCard flight = TripSearchResponse.FlightCard.builder()
                .airline("Mock Air")
                .priceWon(140400)
                .departureAirport("ICN")
                .departureTime("06:05")
                .arrivalAirport("FUK")
                .arrivalTime("07:35")
                .durationMinutes(90)
                .stops(0)
                .segments(List.of(
                        new TripSearchResponse.Segment("TW207", "ICN", "FUK", "06:05", "07:35")
                ))
                .build();

        return TripSearchResponse.builder()
                .requested(req)
                .exchange(TripSearchResponse.Exchange.builder()
                        .base("KRW")
                        .rates(TripSearchResponse.Rates.builder().JPY(0.11).USD(0.00075).build())
                        .updatedAt(LocalDateTime.now().toString())
                        .build())
                .budget(TripSearchResponse.Budget.builder()
                        .budgetWon(req.budgetWon())
                        .estimatedTotalWon(flight.getPriceWon())
                        .remainingWon(Math.max(0, req.budgetWon() - flight.getPriceWon()))
                        .build())
                .flights(List.of(flight))
                .hotels(List.of())
                .build();
    }
}

