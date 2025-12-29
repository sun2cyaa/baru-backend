package com.baru.backend.api;

import com.baru.backend.service.amadeus.AmadeusFlightService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/flights")
public class FlightController {

    private final AmadeusFlightService amadeusFlightService;

    public FlightController(AmadeusFlightService amadeusFlightService) {
        this.amadeusFlightService = amadeusFlightService;
    }

    @GetMapping("/offers")
    public Map<String, Object> offers(
            @RequestParam String originLocationCode,
            @RequestParam String destinationLocationCode,
            @RequestParam String departureDate,
            @RequestParam(required = false) String returnDate,
            @RequestParam(defaultValue = "1") int adults,
            @RequestParam(defaultValue = "5") int max,
            @RequestParam(defaultValue = "KRW") String currencyCode
    ) {
        return amadeusFlightService.searchOffers(
                originLocationCode,
                destinationLocationCode,
                departureDate,
                returnDate,
                adults,
                max,
                currencyCode
        );
    }
}
