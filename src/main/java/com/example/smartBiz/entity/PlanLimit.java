package com.example.smartBiz.entity;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "plan_limits",
        uniqueConstraints = @UniqueConstraint(columnNames = {"plan_id", "limit_key"}))
public class PlanLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    /**
     * Limit key, e.g. INVOICES_PER_MONTH, MAX_USERS, MAX_PRODUCTS
     * Admin defines these dynamically via API.
     */
    @Column(name = "limit_key", nullable = false, length = 100)
    private String limitKey;

    /**
     * Limit value. Use -1 for unlimited.
     */
    @Column(name = "limit_value", nullable = false)
    private Long limitValue;
}
