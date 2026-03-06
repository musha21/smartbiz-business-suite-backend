package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopProductDto {
    private Long productId;
    private String productName; // MUST be exactly "productName"
    private Integer totalQty; // MUST be exactly "totalQty"
    private BigDecimal totalRevenue; // MUST be exactly "totalRevenue"

    // Constructor used by JPQL (3-arg: id, name, qty)
    public TopProductDto(Long productId, String productName, long totalQty) {
        this.productId = productId;
        this.productName = productName;
        this.totalQty = (int) totalQty;
        this.totalRevenue = BigDecimal.ZERO;
    }

    // Constructor used by JPQL (4-arg: id, name, qty, revenue)
    public TopProductDto(Long productId, String productName, long totalQty, double totalRevenue) {
        this.productId = productId;
        this.productName = productName;
        this.totalQty = (int) totalQty;
        this.totalRevenue = BigDecimal.valueOf(totalRevenue);
    }
}
