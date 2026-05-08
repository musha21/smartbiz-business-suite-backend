package com.example.smartBiz.entity;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(
        name = "usage_counters",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"business_id", "usage_period"}
        ),
        indexes = {
                @Index(name = "idx_usage_business", columnList = "business_id"),
                @Index(name = "idx_usage_period", columnList = "usage_period")
        }
)
public class UsageCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    /** Format: YYYY-MM */
    @Column(name = "usage_period", nullable = false, length = 7)
    private String yearMonth;

    @Column(nullable = false)
    private Long invoiceCount = 0L;

    @Column(nullable = false)
    private Long aiCount = 0L;
}