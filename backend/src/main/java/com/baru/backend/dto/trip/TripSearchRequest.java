package com.baru.backend.dto.trip;

public record TripSearchRequest(
        int budgetWon,
        int people,
        String departDate,   // YYYY-MM-DD
        String returnDate,   // YYYY-MM-DD
        String preferredRegion
) {}

