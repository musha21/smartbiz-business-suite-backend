package com.example.smartBiz.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OpenRegisterRequest {
    @NotNull(message = "Business ID is required")
    private Long businessId;

    @NotNull(message = "Cashier ID is required")
    private Long cashierId;

    @NotNull(message = "Opening balance is required")
    @PositiveOrZero(message = "Opening balance must be zero or positive")
    private BigDecimal openingBalance;

    private String notes;
}
