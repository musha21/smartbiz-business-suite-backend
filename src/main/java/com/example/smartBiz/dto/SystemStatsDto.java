package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemStatsDto {
    private StatDetail customers;
    private StatDetail businesses;
    private StatDetail invoices;
    private StatDetail products;
    private String systemUptime;
    private StatDetail aiCreditsUsed;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatDetail {
        private Long value;
        private String trend; // e.g., "+12%"
        private String direction; // "up" or "down"
    }
}
