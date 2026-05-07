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
public class PaymentHistoryDto {

    private String orderId;
    private String planName;
    private Double amount;
    private String currency;
    private String status;
    private LocalDateTime paidAt;
    private String billingCycle;
}
