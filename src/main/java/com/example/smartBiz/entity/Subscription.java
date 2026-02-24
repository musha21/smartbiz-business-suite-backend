package com.example.smartBiz.entity;

import com.example.smartBiz.enums.BillingCycle;
import com.example.smartBiz.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "subscriptions",
        indexes = {
                @Index(name = "idx_sub_business", columnList = "business_id"),
                @Index(name = "idx_sub_plan", columnList = "plan_id")
        })
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── Relationships ───────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    // ─── Subscription details ────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingCycle billingCycle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    /** Subscription start timestamp */
    @Column(nullable = false)
    private LocalDateTime startAt;

    /** null = no expiry (e.g. free plan forever) */
    private LocalDateTime endAt;

    // ─── Stripe fields (nullable for now, ready for later) ──

    private String stripeCustomerId;
    private String stripeSubscriptionId;
    private String stripePriceId;

    // ─── Audit ───────────────────────────────────────

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
