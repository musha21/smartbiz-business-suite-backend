package com.example.smartBiz.controller;

import com.example.smartBiz.service.InvoicePdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/api/invoices")
@CrossOrigin
public class InvoicePdfController {

    private final InvoicePdfService invoicePdfService;

    @Autowired
    public InvoicePdfController(InvoicePdfService invoicePdfService) {
        this.invoicePdfService = invoicePdfService;
    }

    // GET /v1/api/invoices/{id}/pdf
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long id) {

        byte[] pdfBytes = invoicePdfService.generateInvoicePdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("invoice-" + id + ".pdf")
                .build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
