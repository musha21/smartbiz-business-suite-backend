package com.example.smartBiz.controller;

import com.example.smartBiz.dto.BatchCreateDto;
import com.example.smartBiz.dto.ProductBatchDto;
import com.example.smartBiz.service.ProductBatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/api/batches")
public class ProductBatchController {

    private final ProductBatchService batchService;

    public ProductBatchController(ProductBatchService batchService) {
        this.batchService = batchService;
    }

    @PostMapping
    public ResponseEntity<ProductBatchDto> addStock(@RequestBody BatchCreateDto dto) {
        return ResponseEntity.ok(batchService.addStock(dto));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductBatchDto>> getBatches(@PathVariable(name = "productId") Long productId) {
        return ResponseEntity.ok(batchService.getBatchesByProduct(productId));
    }

    @GetMapping("/{batchId}")
    public ResponseEntity<ProductBatchDto> getBatch(@PathVariable(name = "batchId") Long batchId) {
        return ResponseEntity.ok(batchService.getBatchById(batchId));
    }

    @DeleteMapping("/{batchId}")
    public ResponseEntity<Void> delete(@PathVariable(name = "batchId") Long batchId) {
        batchService.deleteBatch(batchId);
        return ResponseEntity.noContent().build();
    }
}
