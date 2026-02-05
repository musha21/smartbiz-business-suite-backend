package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductsDto {

    private Long id;
    private String name;
    private Double price;
    private Integer stockQty;
    private Integer lowStockLimit;
    private Long businessId;// reference to business
    private Long supplierId;

    public ProductsDto(String name, Double price, Integer stockQty, Integer lowStockLimit, Long businessId) {
        this.name = name;
        this.price = price;
        this.stockQty = stockQty;
        this.lowStockLimit = lowStockLimit;
        this.businessId = businessId;
    }

}
