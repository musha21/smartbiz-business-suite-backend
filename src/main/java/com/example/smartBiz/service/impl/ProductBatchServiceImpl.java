package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.BatchCreateDto;
import com.example.smartBiz.dto.ProductBatchDto;
import com.example.smartBiz.entity.ProductBatch;
import com.example.smartBiz.entity.Products;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.ProductBatchRepo;
import com.example.smartBiz.repository.ProductRepo;
import com.example.smartBiz.security.CustomUserPrincipal;
import com.example.smartBiz.service.ProductBatchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductBatchServiceImpl implements ProductBatchService {

    private final ProductBatchRepo batchRepo;
    private final ProductRepo productRepo;

    public ProductBatchServiceImpl(ProductBatchRepo batchRepo, ProductRepo productRepo) {
        this.batchRepo = batchRepo;
        this.productRepo = productRepo;
    }

    private Long requireBusinessId() {
        CustomUserPrincipal principal = CustomUserPrincipal.getCurrent();
        if (principal == null || principal.getBusinessId() == null)
            throw new RuntimeException("Business context missing (JWT required)");
        return principal.getBusinessId();
    }

    private Products requireOwnedProduct(Long productId, Long businessId) {
        Products p = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        if (p.getBusinessId() == null || !p.getBusinessId().equals(businessId)) {
            throw new ResourceNotFoundException("Access denied: product not in your business");
        }
        return p;
    }

    private ProductBatchDto toDto(ProductBatch b) {
        return new ProductBatchDto(
                b.getId(),
                b.getProduct().getId(),
                b.getBatchNumber(),
                b.getQtyAvailable(),
                b.getCreatedAt());
    }

    // ✅ Add stock to batch AND also update product.stock_qty
    @Override
    @Transactional
    public ProductBatchDto addStock(BatchCreateDto dto) {

        Long businessId = requireBusinessId();

        if (dto.getProductId() == null)
            throw new ResourceNotFoundException("productId is required");
        if (dto.getBatchNumber() == null || dto.getBatchNumber().isBlank())
            throw new ResourceNotFoundException("batchNumber is required");
        if (dto.getQty() == null || dto.getQty() <= 0)
            throw new ResourceNotFoundException("qty must be > 0");

        Products product = requireOwnedProduct(dto.getProductId(), businessId);

        // find existing batch or create new
        ProductBatch batch = batchRepo
                .findByBusinessIdAndProduct_IdAndBatchNumber(businessId, product.getId(), dto.getBatchNumber())
                .orElseGet(() -> {
                    ProductBatch nb = new ProductBatch();
                    nb.setBusinessId(businessId);
                    nb.setProduct(product);
                    nb.setBatchNumber(dto.getBatchNumber());
                    nb.setQtyAvailable(0);
                    return nb;
                });

        int addQty = dto.getQty();

        Integer currentBatchQty = batch.getQtyAvailable();
        if (currentBatchQty == null)
            currentBatchQty = 0;
        batch.setQtyAvailable(currentBatchQty + addQty);
        ProductBatch savedBatch = batchRepo.save(batch);

        // ✅ update product total stock too
        Integer productStock = product.getStock_qty();
        if (productStock == null)
            productStock = 0;
        product.setStock_qty(productStock + addQty);
        productRepo.save(product);

        return toDto(savedBatch);
    }

    @Override
    public List<ProductBatchDto> getBatchesByProduct(Long productId) {
        Long businessId = requireBusinessId();
        requireOwnedProduct(productId, businessId); // security guard

        return batchRepo.findByBusinessIdAndProduct_IdOrderByCreatedAtDesc(businessId, productId)
                .stream().map(this::toDto).toList();
    }

    @Override
    public ProductBatchDto getBatchById(Long batchId) {
        Long businessId = requireBusinessId();

        ProductBatch b = batchRepo.findByIdAndBusinessId(batchId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));

        return toDto(b);
    }

    @Override
    @Transactional
    public void deleteBatch(Long batchId) {
        Long businessId = requireBusinessId();

        ProductBatch b = batchRepo.findByIdAndBusinessId(batchId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));

        Integer qty = b.getQtyAvailable();
        if (qty == null)
            qty = 0;
        if (qty > 0) {
            throw new ResourceNotFoundException("Cannot delete batch with stock > 0");
        }

        batchRepo.delete(b);
    }
}
