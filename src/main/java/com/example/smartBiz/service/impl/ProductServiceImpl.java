package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.ProductsDto;
import com.example.smartBiz.entity.Products;
import com.example.smartBiz.entity.Supplier;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.ProductRepo;
import com.example.smartBiz.repository.SupplierRepo;
import com.example.smartBiz.security.RequestContext;
import com.example.smartBiz.service.ProductService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepo productRepo;
    private final SupplierRepo supplierRepo;
    private final RequestContext requestContext;

    public ProductServiceImpl(ProductRepo productRepo, SupplierRepo supplierRepo, RequestContext requestContext) {
        this.productRepo = productRepo;
        this.supplierRepo = supplierRepo;
        this.requestContext = requestContext;
    }

    // -------------------------
    // Helpers (reduce duplicate)
    // -------------------------
    private Long requireBusinessId() {
        Long businessId = requestContext.getBusinessId();
        if (businessId == null) {
            throw new RuntimeException("Business context missing (JWT token required)");
        }
        return businessId;
    }

    private ProductsDto toDto(Products p) {
        return new ProductsDto(
                p.getId(),
                p.getName(),
                p.getPrice(),
                p.getStock_qty(),
                p.getLow_stock_limit(),
                p.getSupplier() != null ? p.getSupplier().getId() : null
        );
    }

    private Products requireOwnedProduct(Long id, Long businessId) {
        Products product = productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + id));

        if (product.getBusinessId() == null || !product.getBusinessId().equals(businessId)) {
            throw new RuntimeException("Access denied: product not in your business");
        }
        return product;
    }

    private void applyDtoToEntity(Products product, ProductsDto dto) {
        product.setName(dto.getName());
        product.setPrice(dto.getPrice());
        product.setStock_qty(dto.getStockQty());
        product.setLow_stock_limit(dto.getLowStockLimit());

        if (dto.getSupplierId() != null) {
            Supplier supplier = supplierRepo.findById(dto.getSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id " + dto.getSupplierId()));
            product.setSupplier(supplier);
        } else {
            product.setSupplier(null);
        }
    }

    // -------------------------
    // CRUD
    // -------------------------
    @Override
    public ProductsDto createProduct(ProductsDto dto) {
        Long businessId = requireBusinessId();

        Products product = new Products();
        applyDtoToEntity(product, dto);

        // ✅ businessId from JWT only
        product.setBusinessId(businessId);

        return toDto(productRepo.save(product));
    }

    @Override
    public ProductsDto updateProduct(Long id, ProductsDto dto) {
        Long businessId = requireBusinessId();

        Products existing = requireOwnedProduct(id, businessId);
        applyDtoToEntity(existing, dto);

        // ✅ never allow changing business ownership
        existing.setBusinessId(businessId);

        return toDto(productRepo.save(existing));
    }

    @Override
    public void deleteProduct(Long id) {
        Long businessId = requireBusinessId();
        Products product = requireOwnedProduct(id, businessId);
        productRepo.delete(product);
    }

    @Override
    public ProductsDto getProductById(Long id) {
        Long businessId = requireBusinessId();
        Products product = requireOwnedProduct(id, businessId);
        return toDto(product);
    }

    @Override
    public List<ProductsDto> getAllProducts() {
        Long businessId = requireBusinessId();

        // ✅ must NOT use findAll()
        return productRepo.findByBusinessId(businessId)
                .stream()
                .map(this::toDto)
                .toList();
    }
}
