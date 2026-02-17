package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class InvoiceItemResponseDto {
    private Long productId;
    private String productName;

    private Long batchId;         // ✅ NEW
    private String batchNumber;   // ✅ NEW (nice)

    private Integer quantity;
    private Double unitPrice;
    private Double lineTotal;
}
