package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ProductBatchDto {
    private Long id;
    private Long productId;
    private String batchNumber;
    private Integer qtyAvailable;
    private LocalDateTime createdAt;
}
