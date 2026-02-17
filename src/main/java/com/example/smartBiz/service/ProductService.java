package com.example.smartBiz.service;

import com.example.smartBiz.dto.ProductsDto;

import java.util.List;

public interface ProductService {

    ProductsDto createProduct(ProductsDto dto);

    ProductsDto updateProduct(Long id, ProductsDto dto);

    void deleteProduct(Long id); // will archive internally

    ProductsDto getProductById(Long id);

    List<ProductsDto> getAllProducts();

    // ✅ Soft delete actions
    void archiveProduct(Long id);

    void restoreProduct(Long id);

    List<ProductsDto> getArchivedProducts();
}
