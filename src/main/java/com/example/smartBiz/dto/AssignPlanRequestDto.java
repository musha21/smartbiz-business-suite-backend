package com.example.smartBiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignPlanRequestDto {

    @NotNull
    private Long businessId;

    @NotNull
    private Long planId;

    @NotBlank
    private String billingCycle;  // MONTHLY or YEARLY

    /**
     * Duration count, e.g. 1, 3, 6, 12
     * Ignored for free plans (endAt = null)
     */
    @NotNull
    private Integer durationCount;

    /**
     * Duration unit: MONTHS or YEARS
     * Ignored for free plans (endAt = null)
     */
    @NotBlank
    private String durationUnit;
}
