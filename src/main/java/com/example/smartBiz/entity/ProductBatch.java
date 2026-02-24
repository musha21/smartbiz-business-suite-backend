package com.example.smartBiz.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "product_batches", uniqueConstraints = {
                @UniqueConstraint(name = "uk_batch_per_business_product", columnNames = { "business_id", "product_id",
                                "batch_number" })
}, indexes = {
                @Index(name = "idx_batch_business_product", columnList = "business_id, product_id")
})
public class ProductBatch {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        // Multi-tenant guard
        @Column(name = "business_id", nullable = false)
        private Long businessId;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "product_id", nullable = false)
        private Products product;

        @Column(name = "batch_number", nullable = false, length = 60)
        private String batchNumber;

        @Column(name = "qty_available", nullable = false)
        private Integer qtyAvailable = 0;

        @Column(name = "created_at", nullable = false)
        private LocalDateTime createdAt = LocalDateTime.now();
}
