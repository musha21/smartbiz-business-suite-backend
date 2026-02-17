package com.example.smartBiz.dto;

import lombok.Data;

@Data
public class ProductBatchQtyUpdateDto {
    private Integer qtyChange; // +10 add stock, -2 reduce stock
}
