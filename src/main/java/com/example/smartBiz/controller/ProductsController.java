package com.example.smartBiz.controller;

import com.example.smartBiz.dto.ProductsDto;
import com.example.smartBiz.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/api/products")
@CrossOrigin
public class ProductsController {

    private final ProductService productService;

    public ProductsController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductsDto> create(@RequestBody ProductsDto dto) {
        return ResponseEntity.ok(productService.createProduct(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductsDto> update(@PathVariable Long id, @RequestBody ProductsDto dto) {
        return ResponseEntity.ok(productService.updateProduct(id, dto));
    }

    // ✅ delete = archive
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductsDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping
    public ResponseEntity<List<ProductsDto>> getAll() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    // ✅ soft delete endpoints
    @PatchMapping("/{id}/archive")
    public ResponseEntity<Void> archive(@PathVariable Long id) {
        productService.archiveProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable Long id) {
        productService.restoreProduct(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/archived")
    public ResponseEntity<List<ProductsDto>> getArchived() {
        return ResponseEntity.ok(productService.getArchivedProducts());
    }
}
