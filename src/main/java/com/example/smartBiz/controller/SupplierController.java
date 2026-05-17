package com.example.smartBiz.controller;

import com.example.smartBiz.dto.SupplierDto;
import com.example.smartBiz.dto.SupplierExtendedDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.smartBiz.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping(("/v1/api/supplier"))
@RestController
@Tag(name = "Suppliers", description = "CRUD + archive/restore for suppliers")
public class SupplierController {
    private final SupplierService supplierService;

    @Autowired
    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @Operation(summary = "Create a supplier")
    @ApiResponse(responseCode = "200", description = "Supplier created")
    @PostMapping
    public ResponseEntity<SupplierDto> createSupplier(
            @RequestBody SupplierDto supplierDto) {
        return ResponseEntity.ok(supplierService.createSupplier(supplierDto));
    }

    @Operation(summary = "Update a supplier")
    @ApiResponse(responseCode = "200", description = "Supplier updated")
    @PutMapping("/{id}")
    public ResponseEntity<SupplierDto> updateSupplier(
            @Parameter(description = "Supplier ID") @PathVariable(name = "id") Long id,
            @RequestBody SupplierDto supplierDto) {
        return ResponseEntity.ok(supplierService.updateSupplier(id, supplierDto));
    }

    @Operation(summary = "List archived suppliers")
    @ApiResponse(responseCode = "200", description = "Archived suppliers returned")
    @GetMapping("/archived")
    public ResponseEntity<List<SupplierDto>> getArchivedSuppliers() {
        return ResponseEntity.ok(supplierService.getArchivedSuppliers());
    }

    @Operation(summary = "Archive a supplier", description = "Soft-deletes a supplier. Requires ADMIN or OWNER role.")
    @ApiResponse(responseCode = "204", description = "Supplier archived")
    @PutMapping("/{id}/archive")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> archiveSupplier(@Parameter(description = "Supplier ID") @PathVariable(name = "id") Long id) {
        supplierService.archiveSupplier(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Restore an archived supplier")
    @ApiResponse(responseCode = "204", description = "Supplier restored")
    @PutMapping("/{id}/restore")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> restoreSupplier(@Parameter(description = "Supplier ID") @PathVariable(name = "id") Long id) {
        supplierService.restoreSupplier(id);
        return ResponseEntity.noContent().build();
    }

    // @DeleteMapping("/{id}")
    // public ResponseEntity<Void> deleteSupplier(@PathVariable(name = "id") Long id) {
    //     supplierService.deleteSupplier(id);
    //     return ResponseEntity.noContent().build();
    // }

    @Operation(summary = "Get supplier by ID")
    @ApiResponse(responseCode = "200", description = "Supplier returned")
    @GetMapping("/{id}")
    public ResponseEntity<SupplierDto> getSupplierById(@Parameter(description = "Supplier ID") @PathVariable(name = "id") Long id) {
        SupplierDto supplier = supplierService.getSupplierById(id);
        return ResponseEntity.ok(supplier);
    }

    @Operation(summary = "List all active suppliers")
    @ApiResponse(responseCode = "200", description = "Suppliers returned")
    @GetMapping
    public ResponseEntity<List<SupplierDto>> getAllSuppliers() {
        return ResponseEntity.ok(supplierService.getAllSuppliers());
    }

    // Extended endpoints for procurement

    @Operation(summary = "Create a supplier with extended procurement fields")
    @ApiResponse(responseCode = "201", description = "Supplier created")
    @PostMapping("/extended")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<SupplierExtendedDto> createSupplierExtended(
            @RequestBody SupplierExtendedDto supplierDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(supplierService.createSupplierExtended(supplierDto));
    }

    @Operation(summary = "Update a supplier with extended procurement fields")
    @ApiResponse(responseCode = "200", description = "Supplier updated")
    @PutMapping("/extended/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<SupplierExtendedDto> updateSupplierExtended(
            @Parameter(description = "Supplier ID") @PathVariable(name = "id") Long id,
            @RequestBody SupplierExtendedDto supplierDto) {
        return ResponseEntity.ok(supplierService.updateSupplierExtended(id, supplierDto));
    }

    @Operation(summary = "Get supplier by ID with extended fields")
    @ApiResponse(responseCode = "200", description = "Supplier returned")
    @GetMapping("/extended/{id}")
    public ResponseEntity<SupplierExtendedDto> getSupplierExtendedById(
            @Parameter(description = "Supplier ID") @PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(supplierService.getSupplierExtendedById(id));
    }

    @Operation(summary = "List all active suppliers with extended fields")
    @ApiResponse(responseCode = "200", description = "Suppliers returned")
    @GetMapping("/extended")
    public ResponseEntity<List<SupplierExtendedDto>> getAllSuppliersExtended() {
        return ResponseEntity.ok(supplierService.getAllSuppliersExtended());
    }
}
