package com.example.smartBiz.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "po_line_items",
        indexes = {
                @Index(name = "idx_po_items", columnList = "po_id"),
                @Index(name = "idx_product_po", columnList = "product_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class POLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private ProductBatch batch;

    @Column(name = "quantity_ordered", nullable = false)
    private Integer quantityOrdered;

    @Column(name = "quantity_received")
    @Builder.Default
    private Integer quantityReceived = 0;

    @Column(name = "unit_cost", precision = 12, scale = 2, nullable = false)
    private BigDecimal unitCost;

    @Column(name = "line_total", precision = 12, scale = 2, nullable = false)
    private BigDecimal lineTotal;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        calculateLineTotal();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateLineTotal();
    }

    public void calculateLineTotal() {
        if (quantityOrdered != null && unitCost != null) {
            this.lineTotal = unitCost.multiply(BigDecimal.valueOf(quantityOrdered));
        }
    }

    public boolean isFullyReceived() {
        return quantityReceived != null && quantityOrdered != null &&
               quantityReceived >= quantityOrdered;
    }

    public boolean isPartiallyReceived() {
        return quantityReceived != null && quantityReceived > 0 &&
               !isFullyReceived();
    }
}
