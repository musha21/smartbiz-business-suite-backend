package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class POLineItemResponseDto {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private Long batchId;
    private String batchNumber;
    private Integer quantityOrdered;
    private Integer quantityReceived;
    private BigDecimal unitCost;
    private BigDecimal lineTotal;
    private Boolean fullyReceived;
}
