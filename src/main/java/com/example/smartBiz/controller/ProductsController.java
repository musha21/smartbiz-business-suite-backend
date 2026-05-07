package com.example.smartBiz.controller;

import com.example.smartBiz.dto.ProductsDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.smartBiz.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/api/products")
@CrossOrigin
@Tag(name = "Products", description = "CRUD + archive/restore for products")
public class ProductsController {

    private final ProductService productService;

    public ProductsController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "Create a product", description = "Creates a new product in the authenticated user's business.")
    @ApiResponse(responseCode = "200", description = "Product created")
    @PostMapping
    public ResponseEntity<ProductsDto> create(@RequestBody ProductsDto dto) {
        return ResponseEntity.ok(productService.createProduct(dto));
    }

    @Operation(summary = "Update a product", description = "Updates an existing product by ID.")
    @ApiResponse(responseCode = "200", description = "Product updated")
    @PutMapping("/{id}")
    public ResponseEntity<ProductsDto> update(@Parameter(description = "Product ID") @PathVariable(name = "id") Long id, @RequestBody ProductsDto dto) {
        return ResponseEntity.ok(productService.updateProduct(id, dto));
    }

    @Operation(summary = "Delete a product", description = "Archives (soft-deletes) a product by ID.")
    @ApiResponse(responseCode = "204", description = "Product deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "Product ID") @PathVariable(name = "id") Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get product by ID")
    @ApiResponse(responseCode = "200", description = "Product returned")
    @GetMapping("/{id}")
    public ResponseEntity<ProductsDto> getById(@Parameter(description = "Product ID") @PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @Operation(summary = "List all active products")
    @ApiResponse(responseCode = "200", description = "Products returned")
    @GetMapping
    public ResponseEntity<List<ProductsDto>> getAll() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @Operation(summary = "Archive a product", description = "Soft-deletes a product.")
    @ApiResponse(responseCode = "204", description = "Product archived")
    @PatchMapping("/{id}/archive")
    public ResponseEntity<Void> archive(@Parameter(description = "Product ID") @PathVariable(name = "id") Long id) {
        productService.archiveProduct(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Restore an archived product")
    @ApiResponse(responseCode = "204", description = "Product restored")
    @PatchMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@Parameter(description = "Product ID") @PathVariable(name = "id") Long id) {
        productService.restoreProduct(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List archived products")
    @ApiResponse(responseCode = "200", description = "Archived products returned")
    @GetMapping("/archived")
    public ResponseEntity<List<ProductsDto>> getArchived() {
        return ResponseEntity.ok(productService.getArchivedProducts());
    }
}
