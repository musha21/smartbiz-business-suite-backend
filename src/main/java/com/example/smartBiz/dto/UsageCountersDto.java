package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageCountersDto {

    // --- Invoices ---
    private Long invoicesUsed;
    private Long invoicesLimit;

    // --- Customers ---
    private Long customersUsed;
    private Long customersLimit;

    // --- Products ---
    private Long productsUsed;
    private Long productsLimit;

    // --- AI Credits ---
    private Long aiUsed;
    private Long aiLimit;

    private String yearMonth;
}
