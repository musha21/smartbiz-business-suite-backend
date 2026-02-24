package com.example.smartBiz.controller;

import com.example.smartBiz.dto.InvoiceCreateRequestDto;
import com.example.smartBiz.dto.InvoiceResponseDto;
import com.example.smartBiz.dto.InvoiceStatusUpdateDto;
import com.example.smartBiz.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/api/invoices")
@CrossOrigin
public class InvoiceController {

    private final InvoiceService invoiceService;

    @Autowired
    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
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

    @PutMapping("/{id}/status")
    public ResponseEntity<String> updateInvoiceStatus(
            @PathVariable(name = "id") Long id,
            @RequestBody InvoiceStatusUpdateDto dto) {
        invoiceService.updateInvoiceStatus(id, dto);
        return ResponseEntity.ok("Invoice status updated successfully");
    }

}
