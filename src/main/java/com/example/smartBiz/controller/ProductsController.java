package com.example.smartBiz.controller;

import com.example.smartBiz.dto.ProductsDto;
import com.example.smartBiz.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/api/products")
public class ProductsController {
    private ProductService productService;

    @Autowired
    public ProductsController(ProductService productService) {
        this.productService = productService;
    }


    @PostMapping
    public ResponseEntity<ProductsDto> createProduct(@RequestBody ProductsDto productDto) {
        ProductsDto createdProduct = productService.createProduct(productDto);
        return ResponseEntity.ok(createdProduct);
    }

    // Update an existing product
    @PutMapping("/{id}")
    public ResponseEntity<ProductsDto> updateProduct(@PathVariable Long id, @RequestBody ProductsDto productDto) {
        ProductsDto updatedProduct = productService.updateProduct(id, productDto);
        return ResponseEntity.ok(updatedProduct);
    }

    // Delete a product
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // Get a product by ID
    @GetMapping("/{id}")
    public ResponseEntity<ProductsDto> getProductById(@PathVariable Long id) {
        ProductsDto product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    // Get all products
    @GetMapping
    public ResponseEntity<List<ProductsDto>> getAllProducts() {
        List<ProductsDto> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }


}
