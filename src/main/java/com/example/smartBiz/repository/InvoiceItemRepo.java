package com.example.smartBiz.repository;

import com.example.smartBiz.dto.TopProductDto;
import com.example.smartBiz.entity.InvoiceItem;
import com.example.smartBiz.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface InvoiceItemRepo extends JpaRepository<InvoiceItem, Long> {
    @Query("""
        SELECT new com.example.smartBiz.dto.TopProductDto(
            p.id,
            p.name,
            COALESCE(SUM(ii.quantity), 0),
            COALESCE(SUM(ii.lineTotal), 0)
        )
        FROM InvoiceItem ii
        JOIN ii.invoice i
        JOIN ii.product p
        WHERE i.status = :paidStatus
          AND i.invoiceDate BETWEEN :start AND :end
        GROUP BY p.id, p.name
        ORDER BY SUM(ii.quantity) DESC
    """)

    List<TopProductDto> findTopProducts(
            InvoiceStatus paidStatus,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );
}
