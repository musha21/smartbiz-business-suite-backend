package com.example.smartBiz.dto;

import lombok.Data;

@Data
public class InvoiceItemResponseDto {
    private Long productId;
    private String productName;

    private Integer quantity;
    private Double unitPrice;
    private Double lineTotal;
}
