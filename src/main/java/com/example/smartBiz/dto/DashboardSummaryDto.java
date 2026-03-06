package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class DashboardSummaryDto {

    // ── Today ──
    private BigDecimal todayRevenue; // Sum of paid invoices created TODAY
    private BigDecimal todayExpenses; // Sum of expenses created TODAY
    private BigDecimal todayProfit; // todayRevenue - todayExpenses

    // ── Month ──
    private BigDecimal monthRevenue; // Sum of paid invoices this calendar month
    private BigDecimal monthExpenses; // Sum of expenses this calendar month
    private BigDecimal monthProfit; // monthRevenue - monthExpenses

    // ── Counts ──
    private Integer unpaidInvoicesCount; // Count of invoices with status = UNPAID
    private Integer lowStockCount; // Count of products where quantity < lowStockLimit

    // ── Margins & Trends ──
    private Double profitMarginPercent; // (todayProfit / todayRevenue) * 100, null-safe
    private Double todayGrowthPercent; // % change vs yesterday's revenue
    private Double todayExpenseChangePercent; // % change vs yesterday's expenses
    private String profitStatus; // "Optimal", "Low", "Critical" based on margin
}
