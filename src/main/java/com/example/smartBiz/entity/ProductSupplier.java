package com.example.smartBiz.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_suppliers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_product_supplier", columnNames = {"product_id", "supplier_id"})
        },
        indexes = {
                @Index(name = "idx_product_primary", columnList = "product_id, is_primary"),
                @Index(name = "idx_supplier_products", columnList = "supplier_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSupplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "is_primary")
    private Boolean isPrimary = false;

    @Column(name = "priority_rank")
    private Integer priorityRank = 1;

    @Column(name = "supplier_sku", length = 50)
    private String supplierSku;

    @Column(name = "unit_cost", precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
