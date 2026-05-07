package com.example.smartBiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequestDto {

    @NotNull(message = "Plan ID is required")
    private Long planId;

    @NotBlank(message = "Billing cycle is required (MONTHLY or YEARLY)")
    private String billingCycle;
}
