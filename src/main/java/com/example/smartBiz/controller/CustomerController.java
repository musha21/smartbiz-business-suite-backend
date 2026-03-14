package com.example.smartBiz.controller;

import com.example.smartBiz.dto.CustomerDto;
import com.example.smartBiz.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(("/v1/api/customer"))
public class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<CustomerDto> createCustomer(
            @RequestBody CustomerDto customerDto) {
        return ResponseEntity.ok(customerService.createCustomer(customerDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerDto> updateCustomer(
            @PathVariable(name = "id") Long id,
            @RequestBody CustomerDto customerDto) {
        return ResponseEntity.ok(customerService.updateCustomer(id, customerDto));
    }

    @GetMapping("/archived")
    public ResponseEntity<List<CustomerDto>> getArchivedCustomers() {
        return ResponseEntity.ok(customerService.getArchivedCustomers());
    }

    @PutMapping("/{id}/archive")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> archiveCustomer(@PathVariable(name = "id") Long id) {
        customerService.archiveCustomer(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/restore")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> restoreCustomer(@PathVariable(name = "id") Long id) {
        customerService.restoreCustomer(id);
        return ResponseEntity.noContent().build();
    }

    // @DeleteMapping("/{id}")
    // public ResponseEntity<Void> deleteCustomer(@PathVariable(name = "id") Long id) {
    //     customerService.deleteCustomer(id);
    //     return ResponseEntity.noContent().build();
    // }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDto> getCustomerById(@PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    @GetMapping
    public ResponseEntity<List<CustomerDto>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }
}
