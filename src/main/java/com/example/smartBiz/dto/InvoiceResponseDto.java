package com.example.smartBiz.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class InvoiceResponseDto {
    private Long id;
    private String invoiceNumber;
    private LocalDateTime invoiceDate;
    private String status;
    private Double totalAmount;
    private Double totalDiscount;

    private CustomerDto customer; // ✅ full customer details

    private List<InvoiceItemResponseDto> items;

    private Boolean archived;
    private LocalDateTime archivedAt;
}
