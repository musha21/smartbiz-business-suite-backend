package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class InvoiceItemRequestDto {
    private Long productId;
    private Long batchId;
    private Integer quantity;
    private Double discountPercentage;
    private Double discountAmount;
}
