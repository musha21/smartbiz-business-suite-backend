package com.example.smartBiz.service;

import com.example.smartBiz.dto.ProductSupplierDto;

import java.util.List;

public interface ProductSupplierService {

    ProductSupplierDto linkProductToSupplier(Long productId, Long supplierId, ProductSupplierDto dto);

    void unlinkProductFromSupplier(Long productId, Long supplierId);

    List<ProductSupplierDto> getSuppliersForProduct(Long productId);

    List<ProductSupplierDto> getProductsForSupplier(Long supplierId);

    ProductSupplierDto setPrimarySupplier(Long productId, Long supplierId);

    List<ProductSupplierDto> getBestSuppliersForProduct(Long productId);
}
