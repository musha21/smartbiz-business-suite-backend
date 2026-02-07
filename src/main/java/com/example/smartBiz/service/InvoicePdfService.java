package com.example.smartBiz.service;

public interface InvoicePdfService {
    byte[] generateInvoicePdf(Long invoiceId);
}
