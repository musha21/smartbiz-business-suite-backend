package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class DashboardSummaryDto {
    private double todayRevenue;      // paid invoices
    private double todayExpenses;
    private double todayProfit;

    private double monthRevenue;      // paid invoices
    private double monthExpenses;
    private double monthProfit;

    private long unpaidInvoicesCount;
    private long lowStockCount;
}
