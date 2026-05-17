package com.example.smartBiz.repository;

import com.example.smartBiz.entity.POLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface POLineItemRepo extends JpaRepository<POLineItem, Long> {

    List<POLineItem> findByPurchaseOrder_Id(Long purchaseOrderId);

    List<POLineItem> findByProduct_IdAndPurchaseOrder_BusinessIdOrderByCreatedAtDesc(Long productId, Long businessId);

    @Query("""
            SELECT li FROM POLineItem li
            JOIN FETCH li.purchaseOrder
            JOIN FETCH li.product
            WHERE li.purchaseOrder.id = :poId
            """)
    List<POLineItem> findByPurchaseOrderIdWithDetails(@Param("poId") Long poId);

    @Query("""
            SELECT SUM(li.quantityReceived) FROM POLineItem li
            WHERE li.product.id = :productId
            AND li.purchaseOrder.status = 'RECEIVED'
            AND li.purchaseOrder.businessId = :businessId
            """)
    Integer sumReceivedQtyByProductAndBusiness(
            @Param("productId") Long productId,
            @Param("businessId") Long businessId);
}
