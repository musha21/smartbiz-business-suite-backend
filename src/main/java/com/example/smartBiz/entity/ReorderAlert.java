package com.example.smartBiz.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reorder_alerts",
        indexes = {
                @Index(name = "idx_business_active", columnList = "business_id, status"),
                @Index(name = "idx_product_alert", columnList = "product_id"),
                @Index(name = "idx_triggered", columnList = "triggered_at")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReorderAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private ProductBatch batch;

    @Column(name = "current_stock", nullable = false)
    private Integer currentStock;

    @Column(name = "reorder_point", nullable = false)
    private Integer reorderPoint;

    @Column(name = "suggested_qty", nullable = false)
    private Integer suggestedQty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suggested_supplier_id")
    private Supplier suggestedSupplier;

    @Column(name = "daily_usage_rate", precision = 10, scale = 2)
    private BigDecimal dailyUsageRate;

    @Column(name = "triggered_at")
    private LocalDateTime triggeredAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private AlertStatus status = AlertStatus.ACTIVE;

    @PrePersist
    protected void onCreate() {
        triggeredAt = LocalDateTime.now();
        if (status == null) {
            status = AlertStatus.ACTIVE;
        }
    }

    public void markOrdered() {
        this.status = AlertStatus.ORDERED;
        this.resolvedAt = LocalDateTime.now();
    }

    public void markDismissed() {
        this.status = AlertStatus.DISMISSED;
        this.resolvedAt = LocalDateTime.now();
    }

    public enum AlertStatus {
        ACTIVE, ORDERED, DISMISSED
    }
}
