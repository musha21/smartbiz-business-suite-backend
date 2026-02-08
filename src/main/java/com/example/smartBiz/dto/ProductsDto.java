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
    private Long supplierId;


}
