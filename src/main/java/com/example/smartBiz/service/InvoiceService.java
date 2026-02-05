package com.example.smartBiz.service;

import com.example.smartBiz.dto.InvoiceCreateRequestDto;
import com.example.smartBiz.dto.InvoiceResponseDto;

public interface InvoiceService {
    InvoiceResponseDto createInvoice(InvoiceCreateRequestDto request);
    InvoiceResponseDto getInvoiceById(Long id);
    InvoiceResponseDto getInvoiceByNumber(String invoiceNumber);
}
