package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.ProductSupplierDto;
import com.example.smartBiz.entity.ProductSupplier;
import com.example.smartBiz.entity.Products;
import com.example.smartBiz.entity.Supplier;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.exception.ValidationException;
import com.example.smartBiz.repository.ProductRepo;
import com.example.smartBiz.repository.ProductSupplierRepo;
import com.example.smartBiz.repository.SupplierRepo;
import com.example.smartBiz.security.CustomUserPrincipal;
import com.example.smartBiz.service.ProductSupplierService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class ProductSupplierServiceImpl implements ProductSupplierService {

    private final ProductSupplierRepo productSupplierRepo;
    private final ProductRepo productRepo;
    private final SupplierRepo supplierRepo;

    public ProductSupplierServiceImpl(ProductSupplierRepo productSupplierRepo,
                                      ProductRepo productRepo,
                                      SupplierRepo supplierRepo) {
        this.productSupplierRepo = productSupplierRepo;
        this.productRepo = productRepo;
        this.supplierRepo = supplierRepo;
    }

    private Long requireBusinessId() {
        CustomUserPrincipal principal = CustomUserPrincipal.getCurrent();
        if (principal == null || principal.getBusinessId() == null) {
            throw new ResourceNotFoundException("Business context missing (JWT required)");
        }
        return principal.getBusinessId();
    }

    @Override
    public ProductSupplierDto linkProductToSupplier(Long productId, Long supplierId, ProductSupplierDto dto) {
        Long businessId = requireBusinessId();

        Products product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Supplier supplier = supplierRepo.findByIdAndBusinessId(supplierId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));

        if (!product.getBusinessId().equals(businessId)) {
            throw new ResourceNotFoundException("Product not in your business");
        }

        boolean exists = productSupplierRepo.existsByProduct_IdAndSupplier_Id(productId, supplierId);
        if (exists) {
            throw new ValidationException("Product is already linked to this supplier");
        }

        Integer nextRank = productSupplierRepo.findByProduct_IdOrderByPriorityRankAsc(productId).size() + 1;

        ProductSupplier link = ProductSupplier.builder()
                .product(product)
                .supplier(supplier)
                .isPrimary(dto.getIsPrimary() != null ? dto.getIsPrimary() : false)
                .priorityRank(dto.getPriorityRank() != null ? dto.getPriorityRank() : nextRank)
                .supplierSku(dto.getSupplierSku())
                .unitCost(dto.getUnitCost())
                .build();

        if (Boolean.TRUE.equals(link.getIsPrimary())) {
            productSupplierRepo.findByProduct_IdAndIsPrimaryTrue(productId)
                    .ifPresent(existingPrimary -> {
                        existingPrimary.setIsPrimary(false);
                        productSupplierRepo.save(existingPrimary);
                    });
        }

        ProductSupplier saved = productSupplierRepo.save(link);
        return mapToDto(saved);
    }

    @Override
    public void unlinkProductFromSupplier(Long productId, Long supplierId) {
        Long businessId = requireBusinessId();

        Products product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.getBusinessId().equals(businessId)) {
            throw new ResourceNotFoundException("Product not in your business");
        }

        ProductSupplier link = productSupplierRepo.findByProduct_IdAndSupplier_Id(productId, supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Link not found"));

        productSupplierRepo.delete(link);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSupplierDto> getSuppliersForProduct(Long productId) {
        Long businessId = requireBusinessId();

        Products product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.getBusinessId().equals(businessId)) {
            throw new ResourceNotFoundException("Product not in your business");
        }

        return productSupplierRepo.findByProduct_IdOrderByPriorityRankAsc(productId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSupplierDto> getProductsForSupplier(Long supplierId) {
        Long businessId = requireBusinessId();

        Supplier supplier = supplierRepo.findByIdAndBusinessId(supplierId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));

        return productSupplierRepo.findBySupplier_Id(supplierId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    public ProductSupplierDto setPrimarySupplier(Long productId, Long supplierId) {
        Long businessId = requireBusinessId();

        Products product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.getBusinessId().equals(businessId)) {
            throw new ResourceNotFoundException("Product not in your business");
        }

        ProductSupplier link = productSupplierRepo.findByProduct_IdAndSupplier_Id(productId, supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Product-Supplier link not found"));

        productSupplierRepo.findByProduct_IdAndIsPrimaryTrue(productId)
                .ifPresent(existingPrimary -> {
                    existingPrimary.setIsPrimary(false);
                    productSupplierRepo.save(existingPrimary);
                });

        link.setIsPrimary(true);
        link.setPriorityRank(1);

        List<ProductSupplier> otherLinks = productSupplierRepo.findByProduct_IdOrderByPriorityRankAsc(productId)
                .stream()
                .filter(l -> !l.getId().equals(link.getId()))
                .toList();

        int rank = 2;
        for (ProductSupplier other : otherLinks) {
            other.setPriorityRank(rank++);
            productSupplierRepo.save(other);
        }

        ProductSupplier saved = productSupplierRepo.save(link);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSupplierDto> getBestSuppliersForProduct(Long productId) {
        Long businessId = requireBusinessId();

        Products product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.getBusinessId().equals(businessId)) {
            throw new ResourceNotFoundException("Product not in your business");
        }

        return productSupplierRepo.findBestSuppliersForProduct(productId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    private ProductSupplierDto mapToDto(ProductSupplier link) {
        return ProductSupplierDto.builder()
                .id(link.getId())
                .productId(link.getProduct().getId())
                .productName(link.getProduct().getName())
                .productSku(link.getProduct().getSku())
                .supplierId(link.getSupplier().getId())
                .supplierName(link.getSupplier().getName())
                .isPrimary(link.getIsPrimary())
                .priorityRank(link.getPriorityRank())
                .supplierSku(link.getSupplierSku())
                .unitCost(link.getUnitCost())
                .build();
    }
}
