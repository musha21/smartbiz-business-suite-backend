package com.example.smartBiz.entity;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "usage_counters",
        uniqueConstraints = @UniqueConstraint(columnNames = { "business_id", "year_month" }),
        indexes = {
                @Index(name = "idx_usage_business", columnList = "business_id"),
                @Index(name = "idx_usage_period", columnList = "year_month")
        })
public class UsageCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    /** Format: YYYY-MM, e.g. "2026-02" */
    @Column(name = "`year_month`", nullable = false, length = 7)
    private String yearMonth;

    /** Number of invoices created in this month */
    @Column(nullable = false)
    private Long invoiceCount = 0L;

    /** Number of AI generations in this month */
    @Column(nullable = false)
    private Long aiCount = 0L;
}
