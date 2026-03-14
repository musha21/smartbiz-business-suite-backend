package com.example.smartBiz.controller;

import com.example.smartBiz.dto.SupplierDto;
import com.example.smartBiz.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping(("/v1/api/supplier"))
@RestController
public class SupplierController {
    private final SupplierService supplierService;

    @Autowired
    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping
    public ResponseEntity<SupplierDto> createSupplier(
            @RequestBody SupplierDto supplierDto) {
        return ResponseEntity.ok(supplierService.createSupplier(supplierDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierDto> updateSupplier(
            @PathVariable(name = "id") Long id,
            @RequestBody SupplierDto supplierDto) {
        return ResponseEntity.ok(supplierService.updateSupplier(id, supplierDto));
    }

    @GetMapping("/archived")
    public ResponseEntity<List<SupplierDto>> getArchivedSuppliers() {
        return ResponseEntity.ok(supplierService.getArchivedSuppliers());
    }

    @PutMapping("/{id}/archive")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> archiveSupplier(@PathVariable(name = "id") Long id) {
        supplierService.archiveSupplier(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/restore")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> restoreSupplier(@PathVariable(name = "id") Long id) {
        supplierService.restoreSupplier(id);
        return ResponseEntity.noContent().build();
    }

    // @DeleteMapping("/{id}")
    // public ResponseEntity<Void> deleteSupplier(@PathVariable(name = "id") Long id) {
    //     supplierService.deleteSupplier(id);
    //     return ResponseEntity.noContent().build();
    // }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierDto> getSupplierById(@PathVariable(name = "id") Long id) {
        SupplierDto supplier = supplierService.getSupplierById(id);
        return ResponseEntity.ok(supplier);
    }

    @GetMapping
    public ResponseEntity<List<SupplierDto>> getAllSuppliers() {
        return ResponseEntity.ok(supplierService.getAllSuppliers());
    }
}
