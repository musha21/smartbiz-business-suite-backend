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
public class ProductSupplierDto {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private Long supplierId;
    private String supplierName;
    private Boolean isPrimary;
    private Integer priorityRank;
    private String supplierSku;
    private BigDecimal unitCost;
}
