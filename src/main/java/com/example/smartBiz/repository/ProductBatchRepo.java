package com.example.smartBiz.repository;

import com.example.smartBiz.entity.ProductBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductBatchRepo extends JpaRepository<ProductBatch, Long> {

    List<ProductBatch> findByBusinessIdAndProduct_IdOrderByCreatedAtDesc(Long businessId, Long productId);

    Optional<ProductBatch> findByBusinessIdAndProduct_IdAndBatchNumber(Long businessId, Long productId,
            String batchNumber);

    Optional<ProductBatch> findByIdAndBusinessId(Long id, Long businessId);

    // ✅ Total product stock = SUM(batch.qtyAvailable)
    @Query("""
                SELECT COALESCE(SUM(b.qtyAvailable), 0)
                FROM ProductBatch b
                WHERE b.businessId = :businessId AND b.product.id = :productId
            """)
    Integer sumQtyByBusinessIdAndProductId(
            @org.springframework.data.repository.query.Param("businessId") Long businessId,
            @org.springframework.data.repository.query.Param("productId") Long productId);
}
