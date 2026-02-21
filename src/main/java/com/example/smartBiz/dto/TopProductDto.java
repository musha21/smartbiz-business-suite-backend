package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TopProductDto {
    private Long productId;
    private String productName;

    public TopProductDto(Long productId, String productName, long totalQty) {
        this.productId = productId;
        this.productName = productName;
        this.totalQty = totalQty;
    }

    private long totalQty;
    private double totalSales;
}

