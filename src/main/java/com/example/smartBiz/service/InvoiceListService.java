package com.example.smartBiz.service;

import com.example.smartBiz.dto.InvoiceListDto;

import java.time.LocalDateTime;
import java.util.List;

public interface InvoiceListService {
    List<InvoiceListDto> getAllInvoices(String status, LocalDateTime from, LocalDateTime to, String q);

    List<InvoiceListDto> getInvoicesByCustomer(Long customerId, String status, LocalDateTime from, LocalDateTime to, String q);
}
