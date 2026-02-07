package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.DashboardSummaryDto;
import com.example.smartBiz.enums.InvoiceStatus;
import com.example.smartBiz.repository.ExpenseRepo;
import com.example.smartBiz.repository.InvoiceRepo;
import com.example.smartBiz.repository.ProductRepo;
import com.example.smartBiz.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final InvoiceRepo invoiceRepo;
    private final ProductRepo productRepo;
    private final ExpenseRepo expenseRepository;

    @Autowired
    public DashboardServiceImpl(InvoiceRepo invoiceRepo, ProductRepo productRepo, ExpenseRepo expenseRepository) {
        this.invoiceRepo = invoiceRepo;
        this.productRepo = productRepo;
        this.expenseRepository = expenseRepository;
    }

    @Override
    public DashboardSummaryDto getSummary() {
        // counts
        long unpaidCount = invoiceRepo.countByStatus(InvoiceStatus.UNPAID);
        long lowStockCount = productRepo.countLowStockProducts();

        // today range
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(23, 59, 59);

        double todayRevenue = invoiceRepo.sumTotalAmountByStatusAndDateRange(InvoiceStatus.PAID, todayStart, todayEnd);

        double todayExpenses = expenseRepository.sumExpensesBetween(todayStart, todayEnd);
        double todayProfit = todayRevenue - todayExpenses;

        // month range
        YearMonth ym = YearMonth.now();
        LocalDateTime monthStart = ym.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = ym.atEndOfMonth().atTime(23, 59, 59);

        double monthRevenue = invoiceRepo.sumTotalAmountByStatusAndDateRange(InvoiceStatus.PAID, monthStart, monthEnd);

        double monthExpenses = expenseRepository.sumExpensesBetween(monthStart, monthEnd);
        double monthProfit = monthRevenue - monthExpenses;

        return new DashboardSummaryDto(todayRevenue, todayExpenses, todayProfit, monthRevenue, monthExpenses, monthProfit, unpaidCount, lowStockCount);
    }
}

