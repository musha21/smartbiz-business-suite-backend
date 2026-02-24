package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanResponseDto {

    private Long id;
    private String code;
    private String name;
    private String description;
    private Boolean active;
    private Double monthlyPrice;
    private Double yearlyPrice;
    private String status; // ACTIVE / INACTIVE
    private LocalDateTime createdAt;

    /** All limits for this plan: key → value (-1 = unlimited) */
    private Map<String, Long> limits;
}
