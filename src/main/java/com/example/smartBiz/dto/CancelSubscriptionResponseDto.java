package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelSubscriptionResponseDto {

    /** "CANCELING" (grace period) or "CANCELED" (immediate) */
    private String status;

    /** Human-readable message */
    private String message;

    /** When the cancellation was requested */
    private LocalDateTime canceledAt;

    /** When access ends (null if immediately canceled) */
    private LocalDateTime accessUntil;
}
