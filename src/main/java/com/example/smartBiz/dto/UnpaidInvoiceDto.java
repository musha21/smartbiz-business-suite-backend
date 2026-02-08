package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UnpaidInvoiceDto {
    private Long id;
    private String invoiceNumber;
    private LocalDateTime invoiceDate;
    private double totalAmount;
    private Long customerId;
    private String customerName;
}
