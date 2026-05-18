package com.example.smartBiz.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DailyReportResponse {
    private Long sessionId;
    private Long cashierId;
    private LocalDateTime openTime;
    private LocalDateTime closeTime;
    private String status;

    // Cash Flow
    private BigDecimal openingBalance;
    private BigDecimal cashSales;
    private BigDecimal cardSales;
    private BigDecimal otherSales;
    private BigDecimal totalSales;
    private BigDecimal totalExpenses;
    private BigDecimal expectedCash;
    private BigDecimal actualCash;
    private BigDecimal variance;

    // Transaction Counts
    private int cashTransactionCount;
    private int cardTransactionCount;
    private int expenseCount;

    // Details
    private List<RegisterTransactionResponse> expenses;
    private List<RegisterTransactionResponse> cashTransactions;

    public void calculateTotals() {
        this.totalSales = (cashSales != null ? cashSales : BigDecimal.ZERO)
                .add(cardSales != null ? cardSales : BigDecimal.ZERO)
                .add(otherSales != null ? otherSales : BigDecimal.ZERO);
    }
}
