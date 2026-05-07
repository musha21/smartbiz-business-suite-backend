package com.example.smartBiz.controller;

import com.example.smartBiz.service.InvoicePdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/api/invoices")
@CrossOrigin
@Tag(name = "Invoice PDF", description = "Download & preview invoice PDFs")
public class InvoicePdfController {

    private final InvoicePdfService invoicePdfService;

    @Autowired
    public InvoicePdfController(InvoicePdfService invoicePdfService) {
        this.invoicePdfService = invoicePdfService;
    }

    @Operation(summary = "Download invoice PDF", description = "Generates and downloads the invoice as a PDF file (Content-Disposition: attachment).")
    @ApiResponse(responseCode = "200", description = "PDF file returned")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(@Parameter(description = "Invoice ID") @PathVariable(name = "id") Long id) {

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

    @Operation(summary = "Preview invoice PDF", description = "Generates and returns the invoice PDF inline for browser preview (Content-Disposition: inline).")
    @ApiResponse(responseCode = "200", description = "PDF preview returned")
    @GetMapping("/{id}/pdf/preview")
    public ResponseEntity<byte[]> previewInvoicePdf(@Parameter(description = "Invoice ID") @PathVariable(name = "id") Long id) {

        byte[] pdfBytes = invoicePdfService.generateInvoicePdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.inline()
                        .filename("invoice-" + id + ".pdf")
                        .build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

}
