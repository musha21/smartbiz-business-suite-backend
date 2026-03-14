package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopProductDto {
    private Long productId;
    private String productName; // MUST be exactly "productName"
    private Integer totalQty; // MUST be exactly "totalQty"
    private Double totalRevenue; // MUST be exactly "totalRevenue"

    // Constructor used by JPQL (3-arg: id, name, qty)
    public TopProductDto(Long productId, String productName, long totalQty) {
        this.productId = productId;
        this.productName = productName;
        this.totalQty = (int) totalQty;
        this.totalRevenue = 0.0;
    }

    // Constructor used by JPQL (4-arg: id, name, qty, revenue)
    public TopProductDto(Long productId, String productName, long totalQty, double totalRevenue) {
        this.productId = productId;
        this.productName = productName;
        this.totalQty = (int) totalQty;
        this.totalRevenue = totalRevenue;
    }
}
