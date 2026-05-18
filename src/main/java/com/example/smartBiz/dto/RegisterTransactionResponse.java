package com.example.smartBiz.dto;

import com.example.smartBiz.entity.RegisterTransaction;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RegisterTransactionResponse {
    private Long id;
    private Long sessionId;
    private Long businessId;
    private RegisterTransaction.TransactionType type;
    private BigDecimal amount;
    private RegisterTransaction.PaymentMethod paymentMethod;
    private String description;
    private String referenceNumber;
    private String receiptUrl;
    private LocalDateTime timestamp;
    private LocalDateTime createdAt;

    public static RegisterTransactionResponse fromEntity(RegisterTransaction transaction) {
        RegisterTransactionResponse response = new RegisterTransactionResponse();
        response.setId(transaction.getId());
        response.setSessionId(transaction.getSessionId());
        response.setBusinessId(transaction.getBusinessId());
        response.setType(transaction.getType());
        response.setAmount(transaction.getAmount());
        response.setPaymentMethod(transaction.getPaymentMethod());
        response.setDescription(transaction.getDescription());
        response.setReferenceNumber(transaction.getReferenceNumber());
        response.setReceiptUrl(transaction.getReceiptUrl());
        response.setTimestamp(transaction.getTimestamp());
        response.setCreatedAt(transaction.getCreatedAt());
        return response;
    }
}
