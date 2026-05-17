package com.example.smartBiz.repository;

import com.example.smartBiz.dto.TopProductDto;
import com.example.smartBiz.entity.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

import com.example.smartBiz.repository.projection.TopProductRow;

public interface InvoiceItemRepo extends JpaRepository<InvoiceItem, Long> {

  @Query("""
          SELECT p.id as productId, p.name as name,
                 SUM(ii.quantity) as qtySold,
                 SUM(ii.lineTotal) as revenue
          FROM InvoiceItem ii
          JOIN ii.invoice inv
          JOIN ii.product p
          WHERE inv.businessId = :businessId
            AND inv.status = com.example.smartBiz.enums.InvoiceStatus.PAID
            AND inv.archived = false
            AND inv.invoiceDate BETWEEN :from AND :to
          GROUP BY p.id, p.name
          ORDER BY SUM(ii.quantity) DESC
      """)
  List<TopProductRow> findTopSellingProducts(
      @Param("businessId") Long businessId,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to,
      Pageable pageable);

  @Query("""
          SELECT new com.example.smartBiz.dto.TopProductDto(
              p.id,
              p.name,
              CAST(SUM(ii.quantity) AS long),
              COALESCE(SUM(ii.lineTotal), 0.0)
          )
          FROM InvoiceItem ii
          JOIN ii.invoice i
          JOIN ii.product p
          WHERE i.status = :status
            AND i.archived = false
            AND i.invoiceDate BETWEEN :start AND :end
            AND i.businessId = :businessId
          GROUP BY p.id, p.name
          ORDER BY SUM(ii.quantity) DESC
      """)
  List<TopProductDto> findTopProductsByBusiness(
      @Param("status") com.example.smartBiz.enums.InvoiceStatus status,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      @Param("businessId") Long businessId,
      Pageable pageable);

  @Query("""
          SELECT ii FROM InvoiceItem ii
          JOIN ii.invoice inv
          WHERE ii.product.id = :productId
            AND inv.businessId = :businessId
            AND inv.status = com.example.smartBiz.enums.InvoiceStatus.PAID
            AND inv.archived = false
            AND inv.invoiceDate >= :since
          """)
  List<InvoiceItem> findRecentSalesByProduct(
      @Param("productId") Long productId,
      @Param("businessId") Long businessId,
      @Param("since") LocalDateTime since);
}