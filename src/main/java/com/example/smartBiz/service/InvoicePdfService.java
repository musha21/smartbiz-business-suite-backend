package com.example.smartBiz.service;

public interface InvoicePdfService {
    byte[] generateInvoicePdf(Long invoiceId);

    /**
     * Generates a print-optimized PDF with page numbers and enhanced formatting.
     * @param invoiceId the invoice ID
     * @return PDF bytes optimized for physical printing
     */
    byte[] generateInvoicePdfForPrint(Long invoiceId);
}
