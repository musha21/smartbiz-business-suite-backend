package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.ProductsDto;
import com.example.smartBiz.entity.Business;
import com.example.smartBiz.entity.Category;
import com.example.smartBiz.entity.Products;
import com.example.smartBiz.entity.Supplier;
import com.example.smartBiz.entity.Subscription;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.*;
import com.example.smartBiz.security.CustomUserPrincipal;
import com.example.smartBiz.service.ProductService;
import com.example.smartBiz.service.PlanLimitService;
import com.example.smartBiz.service.SubscriptionService;
import com.example.smartBiz.service.SequenceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepo productRepo;
    private final SupplierRepo supplierRepo;
    private final ProductBatchRepo batchRepo;

    private final CategoryRepo categoryRepo;
    private final BusinessRepo businessRepo;
    private final SubscriptionService subscriptionService;
    private final PlanLimitService planLimitService;
    private final SequenceService sequenceService;

    public ProductServiceImpl(
            ProductRepo productRepo,
            SupplierRepo supplierRepo,
            ProductBatchRepo batchRepo,
            CategoryRepo categoryRepo,
            BusinessRepo businessRepo,
            SubscriptionService subscriptionService,
            PlanLimitService planLimitService,
            SequenceService sequenceService) {
        this.productRepo = productRepo;
        this.supplierRepo = supplierRepo;
        this.batchRepo = batchRepo;
        this.categoryRepo = categoryRepo;
        this.businessRepo = businessRepo;
        this.subscriptionService = subscriptionService;
        this.planLimitService = planLimitService;
        this.sequenceService = sequenceService;
    }

    private Long requireBusinessId() {
        CustomUserPrincipal principal = CustomUserPrincipal.getCurrent();
        if (principal == null || principal.getBusinessId() == null)
            throw new ResourceNotFoundException("Business context missing (JWT required)");
        return principal.getBusinessId();
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
        if (categoryId == null)
            return null;
        return categoryRepo.findByIdAndBusinessId(categoryId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + categoryId));
    }

    private String prefix(String text, int len) {
        String cleaned = (text == null ? "" : text)
                .replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase();
        if (cleaned.length() >= len)
            return cleaned.substring(0, len);
        return (cleaned + "XXX").substring(0, len);
    }

    private String generateSku(String businessName, String categoryName, Long sequenceValue) {
        String bus = prefix(businessName, 3);
        String cat = prefix(categoryName, 2);
        return bus + cat + sequenceValue;
    }

    private void applyDtoToEntity(Products product, ProductsDto dto, Long businessId) {
        product.setName(dto.getName());
        product.setPrice(dto.getPrice());
        product.setLow_stock_limit(dto.getLowStockLimit());

        // product table stock not real
        product.setStock_qty(0);

        product.setCategory(resolveCategory(dto.getCategoryId(), businessId));

        if (dto.getSupplierId() != null) {
            // ✅ Fix: Verify supplier belongs to this business
            Supplier supplier = supplierRepo.findByIdAndBusinessId(dto.getSupplierId(), businessId)
                    .orElseThrow(
                            () -> new ResourceNotFoundException(
                                    "Supplier not found for this business with id " + dto.getSupplierId()));
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

        // ✅ Plan Limit Check: MAX_PRODUCTS
        Subscription sub = subscriptionService.getBusinessSubscription(businessId);
        if (sub != null && sub.getPlan() != null) {
            Long limit = planLimitService.getLimitValueOrDefault(sub.getPlan().getId(), "MAX_PRODUCTS", -1L);
            if (limit != -1) {
                long currentCount = productRepo.countByBusinessId(businessId);
                if (currentCount >= limit) {
                    throw new ResourceNotFoundException("Product limit reached (" + limit + "). Upgrade your plan.");
                }
            }
        }

        Business business = businessRepo.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found with id " + businessId));

        Products product = new Products();
        applyDtoToEntity(product, dto, businessId);
        product.setBusinessId(businessId);

        Products saved = productRepo.save(product);

        Long nextVal = sequenceService.getNextValue(businessId, "PRODUCT_SKU", 0);
        String catName = saved.getCategory() != null ? saved.getCategory().getName() : "NA";
        String sku = generateSku(business.getName(), catName, nextVal);

        if (productRepo.existsByBusinessIdAndSku(businessId, sku)) {
            sku = sku + "X" + nextVal;
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
