package com.example.smartBiz.repository;

import com.example.smartBiz.entity.ProductSupplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductSupplierRepo extends JpaRepository<ProductSupplier, Long> {

    List<ProductSupplier> findByProduct_IdOrderByPriorityRankAsc(Long productId);

    List<ProductSupplier> findBySupplier_Id(Long supplierId);

    Optional<ProductSupplier> findByProduct_IdAndIsPrimaryTrue(Long productId);

    Optional<ProductSupplier> findByProduct_IdAndSupplier_Id(Long productId, Long supplierId);

    boolean existsByProduct_IdAndSupplier_Id(Long productId, Long supplierId);

    @Query("""
            SELECT ps FROM ProductSupplier ps
            WHERE ps.product.id = :productId AND ps.supplier.businessId = :businessId
            ORDER BY ps.priorityRank ASC
            """)
    List<ProductSupplier> findByProductIdAndBusinessIdOrderByPriority(
            @Param("productId") Long productId,
            @Param("businessId") Long businessId);

    @Query("""
            SELECT ps FROM ProductSupplier ps
            WHERE ps.product.id = :productId AND ps.isPrimary = true
            """)
    Optional<ProductSupplier> findPrimarySupplierForProduct(@Param("productId") Long productId);

    @Query("""
            SELECT ps FROM ProductSupplier ps
            WHERE ps.product.id = :productId
            ORDER BY ps.supplier.reliabilityScore DESC, ps.priorityRank ASC
            """)
    List<ProductSupplier> findBestSuppliersForProduct(@Param("productId") Long productId);
}
