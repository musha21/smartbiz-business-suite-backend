package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceListDto {
    private Long id;
    private String invoiceNumber;
    private LocalDateTime invoiceDate;
    private String status;
    private Double totalAmount;
    private Long customerId;
    private String customerName;
    private Boolean archived;
    private LocalDateTime archivedAt;
}
