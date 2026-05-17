package com.example.smartBiz.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_orders",
        indexes = {
                @Index(name = "idx_business_status", columnList = "business_id, status"),
                @Index(name = "idx_supplier_po", columnList = "supplier_id"),
                @Index(name = "idx_order_date", columnList = "order_date")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "po_number", unique = true, nullable = false, length = 30)
    private String poNumber;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private POStatus status = POStatus.DRAFT;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(name = "expected_delivery")
    private LocalDate expectedDelivery;

    @Column(name = "actual_delivery")
    private LocalDate actualDelivery;

    @Column(name = "subtotal", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(name = "shipping", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal shipping = BigDecimal.ZERO;

    @Column(name = "total", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<POLineItem> lineItems = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = POStatus.DRAFT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addLineItem(POLineItem lineItem) {
        lineItems.add(lineItem);
        lineItem.setPurchaseOrder(this);
    }

    public void removeLineItem(POLineItem lineItem) {
        lineItems.remove(lineItem);
        lineItem.setPurchaseOrder(null);
    }

    public void calculateTotals() {
        this.subtotal = lineItems.stream()
                .map(POLineItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.total = this.subtotal.add(this.tax).add(this.shipping);
    }

    public enum POStatus {
        DRAFT, SENT, PARTIAL, RECEIVED, CANCELLED
    }
}
