package com.example.smartBiz.repository;

import com.example.smartBiz.entity.Invoice;
import com.example.smartBiz.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepo extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
//List all invoices
    @Query("""
                SELECT i FROM Invoice i
                WHERE (:status IS NULL OR i.status = :status)
                  AND (:from IS NULL OR i.invoiceDate >= :from)
                  AND (:to IS NULL OR i.invoiceDate <= :to)
                  AND (:q IS NULL OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :q, '%')))
                ORDER BY i.invoiceDate DESC
            """)
    List<Invoice> filterInvoices(InvoiceStatus status, LocalDateTime from, LocalDateTime to, String q);

    // ✅ filter invoices by customer
    @Query("""
                SELECT i FROM Invoice i
                WHERE i.customer.id = :customerId
                  AND (:status IS NULL OR i.status = :status)
                  AND (:from IS NULL OR i.invoiceDate >= :from)
                  AND (:to IS NULL OR i.invoiceDate <= :to)
                  AND (:q IS NULL OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :q, '%')))
                ORDER BY i.invoiceDate DESC
            """)
    List<Invoice> filterInvoicesByCustomer(Long customerId, InvoiceStatus status, LocalDateTime from, LocalDateTime to, String q);
}

