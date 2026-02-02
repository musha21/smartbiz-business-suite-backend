package com.example.smartBiz.service;

import com.example.smartBiz.dto.ProductsDto;

import java.util.List;

public interface ProductService {
    ProductsDto createProduct(ProductsDto productDto);

    ProductsDto updateProduct(Long id, ProductsDto productDto);

    void deleteProduct(Long id);

    ProductsDto getProductById(Long id);

    List<ProductsDto> getAllProducts();
}
