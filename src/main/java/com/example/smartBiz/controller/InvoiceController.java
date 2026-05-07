package com.example.smartBiz.controller;

import com.example.smartBiz.dto.InvoiceCreateRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.smartBiz.dto.InvoiceResponseDto;
import com.example.smartBiz.dto.InvoiceStatusUpdateDto;
import com.example.smartBiz.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/api/invoices")
@CrossOrigin
@Tag(name = "Invoices", description = "Create, update, list, archive & restore invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final com.example.smartBiz.service.InvoiceListService invoiceListService;

    @Autowired
    public InvoiceController(InvoiceService invoiceService, com.example.smartBiz.service.InvoiceListService invoiceListService) {
        this.invoiceService = invoiceService;
        this.invoiceListService = invoiceListService;
    }

    @Operation(summary = "Create an invoice", description = "Creates a new invoice with line items for the authenticated user's business.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invoice created"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @PostMapping
    public ResponseEntity<InvoiceResponseDto> createInvoice(@RequestBody InvoiceCreateRequestDto request) {
        return ResponseEntity.ok(invoiceService.createInvoice(request));
    }

    @Operation(summary = "Get invoice by ID", description = "Returns full invoice details including line items.")
    @ApiResponse(responseCode = "200", description = "Invoice returned")
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponseDto> getInvoiceById(@Parameter(description = "Invoice ID") @PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

    @Operation(summary = "Get invoice by number", description = "Looks up an invoice by its unique invoice number string.")
    @ApiResponse(responseCode = "200", description = "Invoice returned")
    @GetMapping("/by-number/{invoiceNumber}")
    public ResponseEntity<InvoiceResponseDto> getInvoiceByNumber(
            @Parameter(description = "Invoice number (e.g. INV-001)") @PathVariable(name = "invoiceNumber") String invoiceNumber) {
        return ResponseEntity.ok(invoiceService.getInvoiceByNumber(invoiceNumber));
    }

    @Operation(summary = "List invoices", description = "Returns all active invoices. Supports optional filters: status, date range, and search query.")
    @ApiResponse(responseCode = "200", description = "Invoices returned")
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

    @Operation(summary = "List archived invoices", description = "Returns all archived (soft-deleted) invoices.")
    @ApiResponse(responseCode = "200", description = "Archived invoices returned")
    @GetMapping("/archived")
    public ResponseEntity<java.util.List<com.example.smartBiz.dto.InvoiceListDto>> getArchivedInvoices() {
        return ResponseEntity.ok(invoiceService.getAllInvoices(true));
    }

    @Operation(summary = "List invoices by customer", description = "Returns invoices for a specific customer with optional filters.")
    @ApiResponse(responseCode = "200", description = "Customer invoices returned")
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<java.util.List<com.example.smartBiz.dto.InvoiceListDto>> getInvoicesByCustomer(
            @PathVariable(name = "customerId") Long customerId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "from", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime from,
            @RequestParam(name = "to", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime to,
            @RequestParam(name = "q", required = false) String q) {
        return ResponseEntity.ok(invoiceListService.getInvoicesByCustomer(customerId, status, from, to, q));
    }

    @Operation(summary = "Update an invoice", description = "Updates invoice details and line items. Requires ADMIN or OWNER role.")
    @ApiResponse(responseCode = "200", description = "Invoice updated")
    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<InvoiceResponseDto> updateInvoice(
            @PathVariable(name = "id") Long id,
            @RequestBody InvoiceCreateRequestDto request) {
        return ResponseEntity.ok(invoiceService.updateInvoice(id, request));
    }

    @Operation(summary = "Update invoice status", description = "Changes the invoice status (e.g. DRAFT -> SENT -> PAID).")
    @ApiResponse(responseCode = "200", description = "Status updated")
    @PutMapping("/{id}/status")
    public ResponseEntity<String> updateInvoiceStatus(
            @PathVariable(name = "id") Long id,
            @RequestBody InvoiceStatusUpdateDto dto) {
        invoiceService.updateInvoiceStatus(id, dto);
        return ResponseEntity.ok("Invoice status updated successfully");
    }

    @Operation(summary = "Archive an invoice", description = "Soft-deletes an invoice by marking it as archived.")
    @ApiResponse(responseCode = "204", description = "Invoice archived")
    @PutMapping("/{id}/archive")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> archiveInvoice(@Parameter(description = "Invoice ID") @PathVariable(name = "id") Long id) {
        invoiceService.archiveInvoice(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Restore an archived invoice", description = "Restores a previously archived invoice.")
    @ApiResponse(responseCode = "204", description = "Invoice restored")
    @PutMapping("/{id}/restore")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> restoreInvoice(@Parameter(description = "Invoice ID") @PathVariable(name = "id") Long id) {
        invoiceService.restoreInvoice(id);
        return ResponseEntity.noContent().build();
    }

}
