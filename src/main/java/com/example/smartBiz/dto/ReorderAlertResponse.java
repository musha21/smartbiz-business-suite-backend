package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderAlertResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private Long batchId;
    private String batchNumber;
    private Integer currentStock;
    private Integer reorderPoint;
    private Integer suggestedQty;
    private Long suggestedSupplierId;
    private String suggestedSupplierName;
    private BigDecimal dailyUsageRate;
    private LocalDateTime triggeredAt;
    private String status;
}
