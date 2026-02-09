package com.example.smartBiz.repository;

import com.example.smartBiz.dto.UnpaidInvoiceDto;
import com.example.smartBiz.entity.Invoice;
import com.example.smartBiz.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepo extends JpaRepository<Invoice, Long> {


    // ✅ List all invoices (BUSINESS SAFE)
    @Query("""
                SELECT i FROM Invoice i
                WHERE i.businessId = :businessId
                  AND (:status IS NULL OR i.status = :status)
                  AND (:from IS NULL OR i.invoiceDate >= :from)
                  AND (:to IS NULL OR i.invoiceDate <= :to)
                  AND (:q IS NULL OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :q, '%')))
                ORDER BY i.invoiceDate DESC
            """)
    List<Invoice> filterInvoices(Long businessId,
                                 InvoiceStatus status,
                                 LocalDateTime from,
                                 LocalDateTime to,
                                 String q);

    // ✅ Filter invoices by customer (BUSINESS SAFE)
    @Query("""
                SELECT i FROM Invoice i
                WHERE i.businessId = :businessId
                  AND i.customer.id = :customerId
                  AND (:status IS NULL OR i.status = :status)
                  AND (:from IS NULL OR i.invoiceDate >= :from)
                  AND (:to IS NULL OR i.invoiceDate <= :to)
                  AND (:q IS NULL OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :q, '%')))
                ORDER BY i.invoiceDate DESC
            """)
    List<Invoice> filterInvoicesByCustomer(Long businessId,
                                           Long customerId,
                                           InvoiceStatus status,
                                           LocalDateTime from,
                                           LocalDateTime to,
                                           String q);

    Long countByStatus(InvoiceStatus status);

    @Query("""
                SELECT COALESCE(SUM(i.totalAmount), 0)
                FROM Invoice i
                WHERE i.status = :status
                  AND i.invoiceDate BETWEEN :start AND :end
            """)
    Double sumTotalAmountByStatusAndDateRange(InvoiceStatus status, LocalDateTime start, LocalDateTime end);

    @Query("""
              SELECT i FROM Invoice i
              LEFT JOIN FETCH i.items it
              LEFT JOIN FETCH it.product
              LEFT JOIN FETCH i.customer
              WHERE i.id = :id
            """)
    Optional<Invoice> findInvoiceForPdf(Long id);

    // Unpaid invoices list (light DTO)
    @Query("""
                SELECT new com.example.smartBiz.dto.UnpaidInvoiceDto(
                    i.id, i.invoiceNumber, i.invoiceDate, i.totalAmount,
                    c.id, c.name
                )
                FROM Invoice i
                JOIN i.customer c
                WHERE i.status = :status
                ORDER BY i.invoiceDate DESC
            """)
    List<UnpaidInvoiceDto> findInvoicesByStatusAsDto(InvoiceStatus status);

    // Monthly revenue (sum of PAID invoice totals)
    @Query("""
                SELECT COALESCE(SUM(i.totalAmount), 0)
                FROM Invoice i
                WHERE i.status = :status
                  AND i.invoiceDate BETWEEN :start AND :end
            """)
    Double sumTotalByStatusBetween(InvoiceStatus status, LocalDateTime start, LocalDateTime end);

    Optional<Invoice> findByInvoiceNumberAndBusinessId(String invoiceNumber, Long businessId);


}

