package com.example.smartBiz.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_register_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashRegisterSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long businessId;

    @Column(nullable = false)
    private Long cashierId;

    @Column(nullable = false)
    private LocalDateTime openTime;

    private LocalDateTime closeTime;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal openingBalance;

    @Column(precision = 19, scale = 4)
    private BigDecimal cashSales;

    @Column(precision = 19, scale = 4)
    private BigDecimal cardSales;

    @Column(precision = 19, scale = 4)
    private BigDecimal otherSales;

    @Column(precision = 19, scale = 4)
    private BigDecimal totalExpenses;

    @Column(precision = 19, scale = 4)
    private BigDecimal expectedCash;

    @Column(precision = 19, scale = 4)
    private BigDecimal actualCash;

    @Column(precision = 19, scale = 4)
    private BigDecimal variance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    private String notes;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public enum SessionStatus {
        OPEN, CLOSED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        openTime = LocalDateTime.now();
        if (cashSales == null) cashSales = BigDecimal.ZERO;
        if (cardSales == null) cardSales = BigDecimal.ZERO;
        if (otherSales == null) otherSales = BigDecimal.ZERO;
        if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateExpectedCash();
    }

    public void calculateExpectedCash() {
        this.expectedCash = openingBalance
                .add(cashSales != null ? cashSales : BigDecimal.ZERO)
                .subtract(totalExpenses != null ? totalExpenses : BigDecimal.ZERO);
    }

    public void calculateVariance() {
        if (actualCash != null && expectedCash != null) {
            this.variance = actualCash.subtract(expectedCash);
        }
    }
}
