package com.baru.backend.api;

import com.baru.backend.dto.trip.TripSearchRequest;
import com.baru.backend.dto.trip.TripSearchResponse;
import com.baru.backend.service.TripSearchService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripSearchService tripSearchService;

    public TripController(TripSearchService tripSearchService) {
        this.tripSearchService = tripSearchService;
    }

    @PostMapping("/search")
    public TripSearchResponse search(@RequestBody TripSearchRequest req) {
        return tripSearchService.search(req);
    }
}
