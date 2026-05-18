package com.example.smartBiz.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CloseRegisterRequest {
    @NotNull(message = "Actual cash amount is required")
    @PositiveOrZero(message = "Actual cash must be zero or positive")
    private BigDecimal actualCash;

    private String notes;
}
