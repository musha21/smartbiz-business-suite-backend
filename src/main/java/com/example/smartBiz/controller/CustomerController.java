package com.example.smartBiz.controller;

import com.example.smartBiz.dto.CustomerDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.smartBiz.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(("/v1/api/customer"))
@Tag(name = "Customers", description = "CRUD + archive/restore for customers")
public class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Operation(summary = "Create a customer")
    @ApiResponse(responseCode = "200", description = "Customer created")
    @PostMapping
    public ResponseEntity<CustomerDto> createCustomer(
            @RequestBody CustomerDto customerDto) {
        return ResponseEntity.ok(customerService.createCustomer(customerDto));
    }

    @Operation(summary = "Update a customer")
    @ApiResponse(responseCode = "200", description = "Customer updated")
    @PutMapping("/{id}")
    public ResponseEntity<CustomerDto> updateCustomer(
            @Parameter(description = "Customer ID") @PathVariable(name = "id") Long id,
            @RequestBody CustomerDto customerDto) {
        return ResponseEntity.ok(customerService.updateCustomer(id, customerDto));
    }

    @Operation(summary = "List archived customers")
    @ApiResponse(responseCode = "200", description = "Archived customers returned")
    @GetMapping("/archived")
    public ResponseEntity<List<CustomerDto>> getArchivedCustomers() {
        return ResponseEntity.ok(customerService.getArchivedCustomers());
    }

    @Operation(summary = "Archive a customer", description = "Soft-deletes a customer. Requires ADMIN or OWNER role.")
    @ApiResponse(responseCode = "204", description = "Customer archived")
    @PutMapping("/{id}/archive")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> archiveCustomer(@Parameter(description = "Customer ID") @PathVariable(name = "id") Long id) {
        customerService.archiveCustomer(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Restore an archived customer")
    @ApiResponse(responseCode = "204", description = "Customer restored")
    @PutMapping("/{id}/restore")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> restoreCustomer(@Parameter(description = "Customer ID") @PathVariable(name = "id") Long id) {
        customerService.restoreCustomer(id);
        return ResponseEntity.noContent().build();
    }

    // @DeleteMapping("/{id}")
    // public ResponseEntity<Void> deleteCustomer(@PathVariable(name = "id") Long id) {
    //     customerService.deleteCustomer(id);
    //     return ResponseEntity.noContent().build();
    // }

    @Operation(summary = "Get customer by ID")
    @ApiResponse(responseCode = "200", description = "Customer returned")
    @GetMapping("/{id}")
    public ResponseEntity<CustomerDto> getCustomerById(@Parameter(description = "Customer ID") @PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    @Operation(summary = "List all active customers")
    @ApiResponse(responseCode = "200", description = "Customers returned")
    @GetMapping
    public ResponseEntity<List<CustomerDto>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }
}
