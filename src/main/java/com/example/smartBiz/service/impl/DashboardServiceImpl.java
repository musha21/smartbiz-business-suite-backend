package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.DashboardSummaryDto;
import com.example.smartBiz.enums.InvoiceStatus;
import com.example.smartBiz.repository.ExpenseRepo;
import com.example.smartBiz.repository.InvoiceRepo;
import com.example.smartBiz.repository.ProductRepo;
import com.example.smartBiz.security.CustomUserPrincipal;
import com.example.smartBiz.service.DashboardService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final InvoiceRepo invoiceRepo;
    private final ProductRepo productRepo;
    private final ExpenseRepo expenseRepo;

    public DashboardServiceImpl(
            InvoiceRepo invoiceRepo,
            ProductRepo productRepo,
            ExpenseRepo expenseRepo) {
        this.invoiceRepo = invoiceRepo;
        this.productRepo = productRepo;
        this.expenseRepo = expenseRepo;
    }

    private Long requireBusinessId() {
        CustomUserPrincipal principal = CustomUserPrincipal.getCurrent();
        if (principal == null || principal.getBusinessId() == null) {
            throw new RuntimeException("Business context missing (JWT required)");
        }
        return principal.getBusinessId();
    }

    @Override
    public DashboardSummaryDto getSummary() {

        Long businessId = requireBusinessId();

        // ── Counts (per business) ──
        long unpaidCount = invoiceRepo.countByBusinessIdAndStatusAndArchivedFalse(businessId, InvoiceStatus.UNPAID);
        long lowStockCount = productRepo.countLowStockProductsByBusinessId(businessId);

        // ── Today range ──
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(23, 59, 59);

        Double todayRevenue = invoiceRepo.sumTotalAmountByBusinessAndStatusAndDateRange(
                businessId, InvoiceStatus.PAID, todayStart, todayEnd);
        Double todayExpenses = expenseRepo.sumExpensesByBusinessBetween(businessId, todayStart, todayEnd);
        
        todayRevenue = todayRevenue == null ? 0.0 : todayRevenue;
        todayExpenses = todayExpenses == null ? 0.0 : todayExpenses;
        double todayProfit = todayRevenue - todayExpenses;

        // ── Yesterday range (for growth %) ──
        LocalDate yesterday = today.minusDays(1);
        LocalDateTime ydayStart = yesterday.atStartOfDay();
        LocalDateTime ydayEnd = yesterday.atTime(23, 59, 59);

        Double ydayRevenue = invoiceRepo.sumTotalAmountByBusinessAndStatusAndDateRange(
                businessId, InvoiceStatus.PAID, ydayStart, ydayEnd);
        Double ydayExpenses = expenseRepo.sumExpensesByBusinessBetween(businessId, ydayStart, ydayEnd);
        
        ydayRevenue = ydayRevenue == null ? 0.0 : ydayRevenue;
        ydayExpenses = ydayExpenses == null ? 0.0 : ydayExpenses;

        // ── Month range ──
        YearMonth ym = YearMonth.now();
        LocalDateTime monthStart = ym.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = ym.atEndOfMonth().atTime(23, 59, 59);

        Double monthRevenue = invoiceRepo.sumTotalAmountByBusinessAndStatusAndDateRange(
                businessId, InvoiceStatus.PAID, monthStart, monthEnd);
        Double monthExpenses = expenseRepo.sumExpensesByBusinessBetween(businessId, monthStart, monthEnd);
        
        monthRevenue = monthRevenue == null ? 0.0 : monthRevenue;
        monthExpenses = monthExpenses == null ? 0.0 : monthExpenses;
        double monthProfit = monthRevenue - monthExpenses;

        // ── Profit margin % (division-by-zero safe) ──
        Double profitMarginPercent = safePercentage(todayProfit, todayRevenue);

        // ── Growth % vs yesterday ──
        Double todayGrowthPercent = safeGrowth(todayRevenue, ydayRevenue);
        Double todayExpenseChangePercent = safeGrowth(todayExpenses, ydayExpenses);

        // ── Profit status ──
        String profitStatus = determineProfitStatus(profitMarginPercent);

        return DashboardSummaryDto.builder()
                .todayRevenue(todayRevenue)
                .todayExpenses(todayExpenses)
                .todayProfit(todayProfit)
                .monthRevenue(monthRevenue)
                .monthExpenses(monthExpenses)
                .monthProfit(monthProfit)
                .unpaidInvoicesCount((int) unpaidCount)
                .lowStockCount((int) lowStockCount)
                .profitMarginPercent(profitMarginPercent)
                .todayGrowthPercent(todayGrowthPercent)
                .todayExpenseChangePercent(todayExpenseChangePercent)
                .profitStatus(profitStatus)
                .build();
    }

    // ── Helper: (numerator / denominator) * 100, zero-safe ──
    private Double safePercentage(double numerator, double denominator) {
        if (denominator == 0) {
            return 0.0;
        }
        return (numerator / denominator) * 100.0;
    }

    // ── Helper: ((current - previous) / previous) * 100, zero-safe ──
    private Double safeGrowth(double current, double previous) {
        if (previous == 0) {
            // If there was nothing yesterday, but there is something today, show 100%; else
            // 0%
            if (current > 0) {
                return 100.0;
            }
            return 0.0;
        }
        return ((current - previous) / previous) * 100.0;
    }

    // ── Helper: classify margin into a status string ──
    private String determineProfitStatus(Double marginPercent) {
        if (marginPercent == null || marginPercent < 10) {
            return "Critical";
        } else if (marginPercent < 20) {
            return "Low";
        } else {
            return "Optimal";
        }
    }
}
