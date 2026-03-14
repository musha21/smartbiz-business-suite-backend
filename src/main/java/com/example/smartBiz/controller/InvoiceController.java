package com.example.smartBiz.controller;

import com.example.smartBiz.dto.InvoiceCreateRequestDto;
import com.example.smartBiz.dto.InvoiceResponseDto;
import com.example.smartBiz.dto.InvoiceStatusUpdateDto;
import com.example.smartBiz.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/api/invoices")
@CrossOrigin
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final com.example.smartBiz.service.InvoiceListService invoiceListService;

    @Autowired
    public InvoiceController(InvoiceService invoiceService, com.example.smartBiz.service.InvoiceListService invoiceListService) {
        this.invoiceService = invoiceService;
        this.invoiceListService = invoiceListService;
    }

    @PostMapping
    public ResponseEntity<InvoiceResponseDto> createInvoice(@RequestBody InvoiceCreateRequestDto request) {
        return ResponseEntity.ok(invoiceService.createInvoice(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponseDto> getInvoiceById(@PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

    @GetMapping("/by-number/{invoiceNumber}")
    public ResponseEntity<InvoiceResponseDto> getInvoiceByNumber(
            @PathVariable(name = "invoiceNumber") String invoiceNumber) {
        return ResponseEntity.ok(invoiceService.getInvoiceByNumber(invoiceNumber));
    }

    @GetMapping
    public ResponseEntity<java.util.List<com.example.smartBiz.dto.InvoiceListDto>> getAllInvoices(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "from", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime from,
            @RequestParam(name = "to", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime to,
            @RequestParam(name = "q", required = false) String q) {
        // If status/filters are provided, use InvoiceListService, otherwise use simple active list
        if (status != null || from != null || to != null || q != null) {
            return ResponseEntity.ok(invoiceListService.getAllInvoices(status, from, to, q));
        }
        return ResponseEntity.ok(invoiceService.getAllInvoices(false));
    }

    @GetMapping("/archived")
    public ResponseEntity<java.util.List<com.example.smartBiz.dto.InvoiceListDto>> getArchivedInvoices() {
        return ResponseEntity.ok(invoiceService.getAllInvoices(true));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<java.util.List<com.example.smartBiz.dto.InvoiceListDto>> getInvoicesByCustomer(
            @PathVariable(name = "customerId") Long customerId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "from", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime from,
            @RequestParam(name = "to", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime to,
            @RequestParam(name = "q", required = false) String q) {
        return ResponseEntity.ok(invoiceListService.getInvoicesByCustomer(customerId, status, from, to, q));
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<InvoiceResponseDto> updateInvoice(
            @PathVariable(name = "id") Long id,
            @RequestBody InvoiceCreateRequestDto request) {
        return ResponseEntity.ok(invoiceService.updateInvoice(id, request));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<String> updateInvoiceStatus(
            @PathVariable(name = "id") Long id,
            @RequestBody InvoiceStatusUpdateDto dto) {
        invoiceService.updateInvoiceStatus(id, dto);
        return ResponseEntity.ok("Invoice status updated successfully");
    }

    @PutMapping("/{id}/archive")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> archiveInvoice(@PathVariable(name = "id") Long id) {
        invoiceService.archiveInvoice(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/restore")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> restoreInvoice(@PathVariable(name = "id") Long id) {
        invoiceService.restoreInvoice(id);
        return ResponseEntity.noContent().build();
    }

}
