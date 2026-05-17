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
public class ProcurementDashboardDto {
    private Long activeAlertCount;
    private Long pendingPOCount;
    private Long overduePOCount;
    private List<ReorderAlertResponse> recentAlerts;
    private List<PurchaseOrderResponse> recentPurchaseOrders;
    private List<SupplierPerformanceDto> supplierPerformance;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierPerformanceDto {
        private Long supplierId;
        private String supplierName;
        private Double reliabilityScore;
        private Integer onTimeDeliveries;
        private Integer lateDeliveries;
    }
}
