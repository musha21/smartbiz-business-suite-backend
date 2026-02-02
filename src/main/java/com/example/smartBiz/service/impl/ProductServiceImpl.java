package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.ProductsDto;
import com.example.smartBiz.service.ProductService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {


    @Override
    public ProductsDto createProduct(ProductsDto productDto) {
        return null;
    }

    @Override
    public ProductsDto updateProduct(Long id, ProductsDto productDto) {
        return null;
    }

    @Override
    public void deleteProduct(Long id) {

    }

    @Override
    public ProductsDto getProductById(Long id) {
        return null;
    }

    @Override
    public List<ProductsDto> getAllProducts() {
        return List.of();
    }
}