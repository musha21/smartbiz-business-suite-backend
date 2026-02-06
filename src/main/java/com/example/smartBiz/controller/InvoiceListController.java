package com.example.smartBiz.controller;

import com.example.smartBiz.dto.InvoiceListDto;
import com.example.smartBiz.service.InvoiceListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/v1/api/invoices")
@CrossOrigin
public class InvoiceListController {

    private final InvoiceListService invoiceListService;

    @Autowired
    public InvoiceListController(InvoiceListService invoiceListService) {
        this.invoiceListService = invoiceListService;
    }

    // ✅ GET /v1/api/invoices?status=PAID&from=2026-02-01T00:00:00&to=2026-02-06T23:59:59&q=INV
    @GetMapping
    public ResponseEntity<List<InvoiceListDto>> getAllInvoices(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(invoiceListService.getAllInvoices(status, from, to, q));
    }

    // ✅ GET /v1/api/invoices/customer/1?status=UNPAID&q=INV
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<InvoiceListDto>> getInvoicesByCustomer(
            @PathVariable Long customerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(invoiceListService.getInvoicesByCustomer(customerId, status, from, to, q));
    }
}
