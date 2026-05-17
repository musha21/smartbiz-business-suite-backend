package com.example.smartBiz.controller;

import com.example.smartBiz.dto.ProductSupplierDto;
import com.example.smartBiz.service.ProductSupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/api/product-suppliers")
@Tag(name = "Product-Supplier Links", description = "Manage product to supplier relationships and rankings")
public class ProductSupplierController {

    private final ProductSupplierService productSupplierService;

    public ProductSupplierController(ProductSupplierService productSupplierService) {
        this.productSupplierService = productSupplierService;
    }

    @Operation(summary = "Link a product to a supplier")
    @ApiResponse(responseCode = "201", description = "Link created")
    @ApiResponse(responseCode = "400", description = "Link already exists")
    @PostMapping("/products/{productId}/suppliers/{supplierId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ProductSupplierDto> linkProductToSupplier(
            @Parameter(description = "Product ID") @PathVariable Long productId,
            @Parameter(description = "Supplier ID") @PathVariable Long supplierId,
            @RequestBody(required = false) ProductSupplierDto dto) {
        if (dto == null) {
            dto = new ProductSupplierDto();
        }
        ProductSupplierDto response = productSupplierService.linkProductToSupplier(productId, supplierId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Unlink a product from a supplier")
    @ApiResponse(responseCode = "204", description = "Link removed")
    @DeleteMapping("/products/{productId}/suppliers/{supplierId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Void> unlinkProductFromSupplier(
            @Parameter(description = "Product ID") @PathVariable Long productId,
            @Parameter(description = "Supplier ID") @PathVariable Long supplierId) {
        productSupplierService.unlinkProductFromSupplier(productId, supplierId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all suppliers for a product")
    @ApiResponse(responseCode = "200", description = "List of suppliers for product")
    @GetMapping("/products/{productId}/suppliers")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<ProductSupplierDto>> getSuppliersForProduct(
            @Parameter(description = "Product ID") @PathVariable Long productId) {
        List<ProductSupplierDto> response = productSupplierService.getSuppliersForProduct(productId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all products for a supplier")
    @ApiResponse(responseCode = "200", description = "List of products for supplier")
    @GetMapping("/suppliers/{supplierId}/products")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<ProductSupplierDto>> getProductsForSupplier(
            @Parameter(description = "Supplier ID") @PathVariable Long supplierId) {
        List<ProductSupplierDto> response = productSupplierService.getProductsForSupplier(supplierId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Set primary supplier for a product")
    @ApiResponse(responseCode = "200", description = "Primary supplier set")
    @PutMapping("/products/{productId}/primary-supplier/{supplierId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ProductSupplierDto> setPrimarySupplier(
            @Parameter(description = "Product ID") @PathVariable Long productId,
            @Parameter(description = "Supplier ID") @PathVariable Long supplierId) {
        ProductSupplierDto response = productSupplierService.setPrimarySupplier(productId, supplierId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get best suppliers for a product (ranked by reliability)")
    @ApiResponse(responseCode = "200", description = "List of best suppliers")
    @GetMapping("/products/{productId}/best-suppliers")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<ProductSupplierDto>> getBestSuppliersForProduct(
            @Parameter(description = "Product ID") @PathVariable Long productId) {
        List<ProductSupplierDto> response = productSupplierService.getBestSuppliersForProduct(productId);
        return ResponseEntity.ok(response);
    }
}
