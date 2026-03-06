package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponseDto {
    private Metrics metrics;
    private List<TopProductDto> topProducts;
    private List<UnpaidInvoiceDto> unpaidInvoices;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Metrics {
        private long invoiceCount;
        private double totalSales;
        private double totalExpenses;
        private double profit;
        private long activeProductsCount;
        private long lowStockProductsCount;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TopProductDto {
        private String name;
        private int qtySold;
        private double revenue;
    }
}
