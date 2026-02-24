package com.example.smartBiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanLimitUpsertDto {

    @NotBlank
    private String key;    // e.g. INVOICES_PER_MONTH, MAX_USERS

    @NotNull
    private Long value;    // -1 = unlimited
}
