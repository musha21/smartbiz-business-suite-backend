package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.ProductsDto;
import com.example.smartBiz.entity.Business;
import com.example.smartBiz.entity.Category;
import com.example.smartBiz.entity.Products;
import com.example.smartBiz.entity.Supplier;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.*;
import com.example.smartBiz.security.RequestContext;
import com.example.smartBiz.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepo productRepo;
    private final SupplierRepo supplierRepo;
    private final ProductBatchRepo batchRepo;
    private final RequestContext requestContext;

    private final CategoryRepo categoryRepo;
    private final BusinessRepo businessRepo;

    public ProductServiceImpl(
            ProductRepo productRepo,
            SupplierRepo supplierRepo,
            ProductBatchRepo batchRepo,
            RequestContext requestContext,
            CategoryRepo categoryRepo,
            BusinessRepo businessRepo
    ) {
        this.productRepo = productRepo;
        this.supplierRepo = supplierRepo;
        this.batchRepo = batchRepo;
        this.requestContext = requestContext;
        this.categoryRepo = categoryRepo;
        this.businessRepo = businessRepo;
    }

    private Long requireBusinessId() {
        Long businessId = requestContext.getBusinessId();
        if (businessId == null) throw new RuntimeException("Business context missing (JWT token required)");
        return businessId;
    }

    // ✅ Only ACTIVE product
    private Products requireOwnedActiveProduct(Long id, Long businessId) {
        return productRepo.findByIdAndBusinessIdAndDeletedAtIsNull(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + id));
    }

    // ✅ Active or Archived (for archive/restore)
    private Products requireOwnedAnyProduct(Long id, Long businessId) {
        return productRepo.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + id));
    }

    private Category resolveCategory(Long categoryId, Long businessId) {
        if (categoryId == null) return null;
        return categoryRepo.findByIdAndBusinessId(categoryId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + categoryId));
    }

    private String prefix(String text, int len) {
        String cleaned = (text == null ? "" : text)
                .replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase();
        if (cleaned.length() >= len) return cleaned.substring(0, len);
        return (cleaned + "XXX").substring(0, len);
    }

    private String generateSku(String businessName, String categoryName, Long productId) {
        String bus = prefix(businessName, 3);
        String cat = prefix(categoryName, 2);
        return bus + cat + productId;
    }

    private void applyDtoToEntity(Products product, ProductsDto dto, Long businessId) {
        product.setName(dto.getName());
        product.setPrice(dto.getPrice());
        product.setLow_stock_limit(dto.getLowStockLimit());

        // product table stock not real
        product.setStock_qty(0);

        product.setCategory(resolveCategory(dto.getCategoryId(), businessId));

        if (dto.getSupplierId() != null) {
            Supplier supplier = supplierRepo.findById(dto.getSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id " + dto.getSupplierId()));
            product.setSupplier(supplier);
        } else {
            product.setSupplier(null);
        }
    }

    private ProductsDto toDto(Products p, Long businessId) {
        Integer available = batchRepo.sumQtyByBusinessIdAndProductId(businessId, p.getId());

        ProductsDto dto = new ProductsDto();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setPrice(p.getPrice());

        dto.setSku(p.getSku());
        dto.setCategoryId(p.getCategory() != null ? p.getCategory().getId() : null);
        dto.setCategoryName(p.getCategory() != null ? p.getCategory().getName() : null);

        dto.setAvailableStock(available);
        dto.setLowStockLimit(p.getLow_stock_limit());
        dto.setSupplierId(p.getSupplier() != null ? p.getSupplier().getId() : null);

        return dto;
    }

    @Override
    @Transactional
    public ProductsDto createProduct(ProductsDto dto) {
        Long businessId = requireBusinessId();

        Business business = businessRepo.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found with id " + businessId));

        Products product = new Products();
        applyDtoToEntity(product, dto, businessId);
        product.setBusinessId(businessId);

        Products saved = productRepo.save(product);

        String catName = saved.getCategory() != null ? saved.getCategory().getName() : "NA";
        String sku = generateSku(business.getName(), catName, saved.getId());

        if (productRepo.existsByBusinessIdAndSku(businessId, sku)) {
            sku = sku + "X";
        }

        saved.setSku(sku);
        Products saved2 = productRepo.save(saved);

        return toDto(saved2, businessId);
    }

    @Override
    @Transactional
    public ProductsDto updateProduct(Long id, ProductsDto dto) {
        Long businessId = requireBusinessId();

        Products existing = requireOwnedActiveProduct(id, businessId);

        String oldSku = existing.getSku();

        applyDtoToEntity(existing, dto, businessId);

        existing.setBusinessId(businessId);
        existing.setSku(oldSku);

        Products saved = productRepo.save(existing);
        return toDto(saved, businessId);
    }

    // ✅ DELETE = ARCHIVE (ERP SAFE)
    @Override
    @Transactional
    public void deleteProduct(Long id) {
        archiveProduct(id);
    }

    @Override
    public ProductsDto getProductById(Long id) {
        Long businessId = requireBusinessId();
        Products product = requireOwnedActiveProduct(id, businessId);
        return toDto(product, businessId);
    }

    @Override
    public List<ProductsDto> getAllProducts() {
        Long businessId = requireBusinessId();
        return productRepo.findByBusinessIdAndDeletedAtIsNull(businessId)
                .stream()
                .map(p -> toDto(p, businessId))
                .toList();
    }

    // ✅ ARCHIVE
    @Override
    @Transactional
    public void archiveProduct(Long id) {
        Long businessId = requireBusinessId();

        Products product = requireOwnedAnyProduct(id, businessId);

        if (product.getDeletedAt() == null) {
            product.setDeletedAt(LocalDateTime.now());
            productRepo.save(product);
        }
    }

    // ✅ RESTORE
    @Override
    @Transactional
    public void restoreProduct(Long id) {
        Long businessId = requireBusinessId();

        Products product = requireOwnedAnyProduct(id, businessId);

        product.setDeletedAt(null);
        productRepo.save(product);
    }

    @Override
    public List<ProductsDto> getArchivedProducts() {
        Long businessId = requireBusinessId();

        return productRepo.findByBusinessIdAndDeletedAtIsNotNull(businessId)
                .stream()
                .map(p -> toDto(p, businessId))
                .toList();
    }
}
