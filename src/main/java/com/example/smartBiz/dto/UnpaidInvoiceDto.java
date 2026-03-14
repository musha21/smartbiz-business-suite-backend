package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnpaidInvoiceDto {

    private Long id;
    private String invoiceNumber;
    private LocalDateTime invoiceDate;
    private Double totalAmount;
    private String customerName; // flat string, NOT a nested customer object

    // Constructor used by JPQL query (includes customerId for backward compat)
    public UnpaidInvoiceDto(Long id, String invoiceNumber, LocalDateTime invoiceDate,
            double totalAmount, Long customerId, String customerName) {
        this.id = id;
        this.invoiceNumber = invoiceNumber;
        this.invoiceDate = invoiceDate;
        this.totalAmount = totalAmount;
        this.customerName = customerName;
        // customerId intentionally not stored — frontend only needs customerName
    }
}