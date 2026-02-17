package com.example.smartBiz.repository;

import com.example.smartBiz.entity.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepo extends JpaRepository<Products, Long> {

    // ✅ Active products only (main listing)
    List<Products> findByBusinessIdAndDeletedAtIsNull(Long businessId);

    // ✅ Find ANY product (active or archived)
    Optional<Products> findByIdAndBusinessId(Long id, Long businessId);

    // ✅ Find ONLY active product
    Optional<Products> findByIdAndBusinessIdAndDeletedAtIsNull(Long id, Long businessId);

    // ✅ SKU check
    boolean existsByBusinessIdAndSku(Long businessId, String sku);

    // ✅ Low stock count (ACTIVE only)
    @Query("""
        SELECT COUNT(p)
        FROM Products p
        WHERE p.businessId = :businessId
          AND p.deletedAt IS NULL
          AND p.stock_qty <= p.low_stock_limit
    """)
    long countLowStockProductsByBusinessId(Long businessId);

    // ✅ Optional: Archived list (if you want archive page)
    List<Products> findByBusinessIdAndDeletedAtIsNotNull(Long businessId);
}
