package com.example.smartBiz.dto;

import lombok.Data;

@Data
public class BatchResponseDto {
    private Long id;
    private Long productId;
    private String batchNumber;
    private Integer qtyAvailable;
}
