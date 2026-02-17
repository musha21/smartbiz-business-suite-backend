package com.example.smartBiz.dto;

import lombok.Data;

@Data
public class BatchCreateDto {
    private Long productId;
    private String batchNumber;
    private Integer qty; // qty to set (or add)
}
