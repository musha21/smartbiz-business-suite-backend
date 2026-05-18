package com.example.smartBiz.dto;

import com.example.smartBiz.entity.CashRegisterSession;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CashRegisterSessionResponse {
    private Long id;
    private Long businessId;
    private Long cashierId;
    private LocalDateTime openTime;
    private LocalDateTime closeTime;
    private BigDecimal openingBalance;
    private BigDecimal cashSales;
    private BigDecimal cardSales;
    private BigDecimal otherSales;
    private BigDecimal totalExpenses;
    private BigDecimal expectedCash;
    private BigDecimal actualCash;
    private BigDecimal variance;
    private CashRegisterSession.SessionStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CashRegisterSessionResponse fromEntity(CashRegisterSession session) {
        CashRegisterSessionResponse response = new CashRegisterSessionResponse();
        response.setId(session.getId());
        response.setBusinessId(session.getBusinessId());
        response.setCashierId(session.getCashierId());
        response.setOpenTime(session.getOpenTime());
        response.setCloseTime(session.getCloseTime());
        response.setOpeningBalance(session.getOpeningBalance());
        response.setCashSales(session.getCashSales());
        response.setCardSales(session.getCardSales());
        response.setOtherSales(session.getOtherSales());
        response.setTotalExpenses(session.getTotalExpenses());
        response.setExpectedCash(session.getExpectedCash());
        response.setActualCash(session.getActualCash());
        response.setVariance(session.getVariance());
        response.setStatus(session.getStatus());
        response.setNotes(session.getNotes());
        response.setCreatedAt(session.getCreatedAt());
        response.setUpdatedAt(session.getUpdatedAt());
        return response;
    }
}
