package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MySubscriptionDto {

    private Long businessId;

    // ─── Plan info ───────────────────────────────
    private Long planId;
    private String planCode;
    private String planName;

    // ─── Subscription info ───────────────────────
    private String status; // ACTIVE / EXPIRED / CANCELED / NONE
    private String billingCycle; // MONTHLY / YEARLY
    private LocalDateTime startAt;
    private LocalDateTime endAt; // null = no expiry

    // ─── Cancellation info ───────────────────────
    private LocalDateTime canceledAt;  // non-null = cancel was requested
    private boolean canceling;         // true = cancel requested, still has access (grace period)

    // ─── Plan limits (from plan_limits table) ────
    /** All limits: key → value (-1 = unlimited) */
    private Map<String, Long> limits;

    // ─── Current month usage ─────────────────────
    private String yearMonth; // e.g. "2026-02"
    private Long invoicesUsed;
}
