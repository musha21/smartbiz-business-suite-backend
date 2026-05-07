package com.example.smartBiz.controller;

import com.example.smartBiz.dto.BatchCreateDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.smartBiz.dto.ProductBatchDto;
import com.example.smartBiz.service.ProductBatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/api/batches")
@Tag(name = "Product Batches", description = "Stock batch management for products")
public class ProductBatchController {

    private final ProductBatchService batchService;

    public ProductBatchController(ProductBatchService batchService) {
        this.batchService = batchService;
    }

    @Operation(summary = "Add stock batch", description = "Creates a new stock batch for a product with quantity, cost price, and expiry date.")
    @ApiResponse(responseCode = "200", description = "Batch created")
    @PostMapping
    public ResponseEntity<ProductBatchDto> addStock(@RequestBody BatchCreateDto dto) {
        return ResponseEntity.ok(batchService.addStock(dto));
    }

    @Operation(summary = "List batches by product", description = "Returns all stock batches for the given product.")
    @ApiResponse(responseCode = "200", description = "Batches returned")
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductBatchDto>> getBatches(@Parameter(description = "Product ID") @PathVariable(name = "productId") Long productId) {
        return ResponseEntity.ok(batchService.getBatchesByProduct(productId));
    }

    @Operation(summary = "Get batch by ID")
    @ApiResponse(responseCode = "200", description = "Batch returned")
    @GetMapping("/{batchId}")
    public ResponseEntity<ProductBatchDto> getBatch(@Parameter(description = "Batch ID") @PathVariable(name = "batchId") Long batchId) {
        return ResponseEntity.ok(batchService.getBatchById(batchId));
    }

    @Operation(summary = "Delete a batch", description = "Permanently deletes a stock batch.")
    @ApiResponse(responseCode = "204", description = "Batch deleted")
    @DeleteMapping("/{batchId}")
    public ResponseEntity<Void> delete(@Parameter(description = "Batch ID") @PathVariable(name = "batchId") Long batchId) {
        batchService.deleteBatch(batchId);
        return ResponseEntity.noContent().build();
    }
}
