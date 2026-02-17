package com.example.smartBiz.service;


import com.example.smartBiz.dto.BatchCreateDto;
import com.example.smartBiz.dto.ProductBatchDto;

import java.util.List;

public interface ProductBatchService {
    ProductBatchDto addStock(BatchCreateDto dto);
    List<ProductBatchDto> getBatchesByProduct(Long productId);
    ProductBatchDto getBatchById(Long batchId);
    void deleteBatch(Long batchId);
}
