package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderResponse {
    private Long id;
    private String poNumber;
    private Long supplierId;
    private String supplierName;
    private String status;
    private LocalDate orderDate;
    private LocalDate expectedDelivery;
    private LocalDate actualDelivery;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal shipping;
    private BigDecimal total;
    private String notes;
    private List<POLineItemResponseDto> lineItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
