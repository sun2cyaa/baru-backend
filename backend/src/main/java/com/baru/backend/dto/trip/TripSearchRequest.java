package com.baru.backend.dto.trip;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripSearchRequest {
    private int budgetWon;        // 예산(원)
    private int people;           // 인원
    private String departDate;    // YYYY-MM-DD
    private String returnDate;    // YYYY-MM-DD

    // 선택 파라미터(프론트가 아직 안 보내도 기본값 처리)
    private Boolean domestic;             // 국내=true, 국외=false
    private String originLocationCode;    // 출발공항(예: ICN)
    private String destinationLocationCode; // 귀국공항(예: GMP)
}
