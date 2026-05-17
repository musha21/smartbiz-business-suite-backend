package com.example.smartBiz.repository;

import com.example.smartBiz.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepo extends JpaRepository<PurchaseOrder, Long> {

    Page<PurchaseOrder> findByBusinessIdOrderByCreatedAtDesc(Long businessId, Pageable pageable);

    List<PurchaseOrder> findByBusinessIdAndStatusOrderByCreatedAtDesc(Long businessId, PurchaseOrder.POStatus status);

    Optional<PurchaseOrder> findByIdAndBusinessId(Long id, Long businessId);

    Optional<PurchaseOrder> findByPoNumber(String poNumber);

    List<PurchaseOrder> findBySupplier_IdAndBusinessIdOrderByCreatedAtDesc(Long supplierId, Long businessId);

    @Query("""
            SELECT po FROM PurchaseOrder po
            LEFT JOIN FETCH po.supplier s
            WHERE po.businessId = :businessId
            """)
    Page<PurchaseOrder> findByBusinessIdWithSupplier(@Param("businessId") Long businessId, Pageable pageable);

    @Query("""
            SELECT po FROM PurchaseOrder po
            LEFT JOIN FETCH po.supplier s
            WHERE po.businessId = :businessId
            AND po.status IN ('SENT', 'PARTIAL')
            AND po.expectedDelivery < :date
            ORDER BY po.expectedDelivery ASC
            """)
    List<PurchaseOrder> findOverdueOrders(
            @Param("businessId") Long businessId,
            @Param("date") LocalDate date);

    @Query("""
            SELECT po FROM PurchaseOrder po
            LEFT JOIN FETCH po.supplier s
            LEFT JOIN FETCH po.lineItems
            WHERE po.id = :id AND po.businessId = :businessId
            """)
    Optional<PurchaseOrder> findByIdWithLineItems(
            @Param("id") Long id,
            @Param("businessId") Long businessId);

    @Query("""
            SELECT po FROM PurchaseOrder po
            LEFT JOIN FETCH po.supplier s
            WHERE po.businessId = :businessId
            AND po.status = :status
            ORDER BY po.createdAt DESC
            """)
    List<PurchaseOrder> findByBusinessIdAndStatusWithSupplier(
            @Param("businessId") Long businessId,
            @Param("status") PurchaseOrder.POStatus status);

    @Query("""
            SELECT COUNT(po) FROM PurchaseOrder po
            WHERE po.businessId = :businessId AND po.status = :status
            """)
    Long countByBusinessIdAndStatus(
            @Param("businessId") Long businessId,
            @Param("status") PurchaseOrder.POStatus status);

    @Query(value = """
            SELECT po.* FROM purchase_orders po
            WHERE po.business_id = :businessId
            AND po.supplier_id = :supplierId
            AND po.actual_delivery > po.expected_delivery
            ORDER BY po.actual_delivery DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<PurchaseOrder> findRecentLateDeliveriesBySupplier(
            @Param("businessId") Long businessId,
            @Param("supplierId") Long supplierId,
            @Param("limit") int limit);

    @Query(value = """
            SELECT MAX(CAST(SUBSTRING(po.po_number, 12) AS UNSIGNED))
            FROM purchase_orders po
            WHERE po.po_number LIKE CONCAT('PO-', :year, '-%')
            """, nativeQuery = true)
    Optional<Integer> findMaxSequenceForYear(@Param("year") String year);
}
