package com.example.smartBiz.repository;

import com.example.smartBiz.dto.UnpaidInvoiceDto;
import com.example.smartBiz.entity.Invoice;
import com.example.smartBiz.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepo extends JpaRepository<Invoice, Long> {

        // ✅ List all invoices (BUSINESS SAFE)
        @Query("""
                            SELECT i FROM Invoice i
                            WHERE i.businessId = :businessId
                              AND i.archived = false
                              AND (:status IS NULL OR i.status = :status)
                              AND (:from IS NULL OR i.invoiceDate >= :from)
                              AND (:to IS NULL OR i.invoiceDate <= :to)
                              AND (:q IS NULL OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :q, '%')))
                            ORDER BY i.invoiceDate DESC
                        """)
        List<Invoice> filterInvoices(
                        @Param("businessId") Long businessId,
                        @Param("status") InvoiceStatus status,
                        @Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to,
                        @Param("q") String q);

        // ✅ Filter invoices by customer (BUSINESS SAFE)
        @Query("""
                            SELECT i FROM Invoice i
                            WHERE i.businessId = :businessId
                              AND i.customer.id = :customerId
                              AND i.archived = false
                              AND (:status IS NULL OR i.status = :status)
                              AND (:from IS NULL OR i.invoiceDate >= :from)
                              AND (:to IS NULL OR i.invoiceDate <= :to)
                              AND (:q IS NULL OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :q, '%')))
                            ORDER BY i.invoiceDate DESC
                        """)
        List<Invoice> filterInvoicesByCustomer(
                        @Param("businessId") Long businessId,
                        @Param("customerId") Long customerId,
                        @Param("status") InvoiceStatus status,
                        @Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to,
                        @Param("q") String q);

        Long countByStatus(InvoiceStatus status);

        @Query("""
                            SELECT COALESCE(SUM(i.totalAmount), 0)
                            FROM Invoice i
                            WHERE i.status = :status
                              AND i.archived = false
                              AND i.invoiceDate BETWEEN :start AND :end
                        """)
        Double sumTotalAmountByStatusAndDateRange(
                        @Param("status") InvoiceStatus status,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query("""
                          SELECT i FROM Invoice i
                          LEFT JOIN FETCH i.items it
                          LEFT JOIN FETCH it.product
                          LEFT JOIN FETCH i.customer
                          WHERE i.id = :id
                        """)
        Optional<Invoice> findInvoiceForPdf(@Param("id") Long id);

        // Unpaid invoices list (light DTO)
        @Query("""
                            SELECT new com.example.smartBiz.dto.UnpaidInvoiceDto(
                                i.id, i.invoiceNumber, i.invoiceDate, i.totalAmount,
                                c.id, c.name
                            )
                            FROM Invoice i
                            JOIN i.customer c
                            WHERE i.status = :status
                              AND i.archived = false
                            ORDER BY i.invoiceDate DESC
                        """)
        List<UnpaidInvoiceDto> findInvoicesByStatusAsDto(@Param("status") InvoiceStatus status);

        // Monthly revenue (sum of PAID invoice totals)
        @Query("""
                            SELECT COALESCE(SUM(i.totalAmount), 0)
                            FROM Invoice i
                            WHERE i.status = :status
                              AND i.archived = false
                              AND i.invoiceDate BETWEEN :start AND :end
                        """)
        Double sumTotalByStatusBetween(
                        @Param("status") InvoiceStatus status,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        List<Invoice> findByBusinessIdAndArchivedFalse(Long businessId);

        List<Invoice> findByBusinessIdAndArchivedTrue(Long businessId);

        Optional<Invoice> findByInvoiceNumberAndBusinessId(String invoiceNumber, Long businessId);

        @Query("""
                          SELECT i FROM Invoice i
                          LEFT JOIN FETCH i.items it
                          LEFT JOIN FETCH it.product
                          LEFT JOIN FETCH it.batch
                          LEFT JOIN FETCH i.customer
                          WHERE i.id = :id
                        """)
        Optional<Invoice> findInvoiceWithItems(@Param("id") Long id);

        long countByBusinessIdAndArchivedFalse(Long businessId);

        long countByBusinessIdAndStatusAndArchivedFalse(Long businessId, InvoiceStatus status);

        @org.springframework.data.jpa.repository.Query("""
                            SELECT COALESCE(SUM(i.totalAmount), 0)
                            FROM Invoice i
                            WHERE i.businessId = :businessId
                              AND i.status = :status
                              AND i.archived = false
                              AND i.invoiceDate BETWEEN :start AND :end
                        """)
        Double sumTotalAmountByBusinessAndStatusAndDateRange(
                        @Param("businessId") Long businessId,
                        @Param("status") InvoiceStatus status,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        // ✅ Monthly revenue (PAID) scoped by business
        @Query("""
                            select coalesce(sum(i.totalAmount), 0)
                            from Invoice i
                            where i.status = :status
                              and i.archived = false
                              and i.invoiceDate between :start and :end
                              and i.businessId = :businessId
                        """)
        double sumTotalByStatusBetweenAndBusinessId(
                        @Param("status") InvoiceStatus status,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end,
                        @Param("businessId") Long businessId);

        @Query("""
                            SELECT COALESCE(SUM(i.totalAmount), 0)
                            FROM Invoice i
                            WHERE i.businessId = :businessId
                              AND i.status = 'PAID'
                              AND i.archived = false
                              AND i.invoiceDate BETWEEN :start AND :end
                        """)
        Double sumPaidRevenueByBusinessAndDateRange(
                        @Param("businessId") Long businessId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query("""
                            SELECT new com.example.smartBiz.dto.UnpaidInvoiceDto(
                                i.id,
                                i.invoiceNumber,
                                i.invoiceDate,
                                COALESCE(i.totalAmount, 0.0),
                                c.id,
                                c.name
                            )
                            FROM Invoice i
                            JOIN i.customer c
                            WHERE i.status = :status
                              AND i.archived = false
                              AND i.invoiceDate BETWEEN :start AND :end
                              AND i.businessId = :businessId
                            ORDER BY i.invoiceDate DESC
                        """)
        List<UnpaidInvoiceDto> findInvoicesByStatusAsDtoAndBusinessId(
                        @Param("status") InvoiceStatus status,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end,
                        @Param("businessId") Long businessId,
                        Pageable pageable);

        @Query("SELECT COUNT(i) FROM Invoice i WHERE i.businessId = :businessId AND i.invoiceDate BETWEEN :start AND :end")
        long countByBusinessIdAndInvoiceDateBetween(
                        @Param("businessId") Long businessId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query("SELECT COUNT(i) FROM Invoice i WHERE i.invoiceDate BETWEEN :start AND :end")
        long countByInvoiceDateBetween(
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

}
