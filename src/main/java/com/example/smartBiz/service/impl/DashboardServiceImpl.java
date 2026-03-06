package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.DashboardSummaryDto;
import com.example.smartBiz.enums.InvoiceStatus;
import com.example.smartBiz.repository.ExpenseRepo;
import com.example.smartBiz.repository.InvoiceRepo;
import com.example.smartBiz.repository.ProductRepo;
import com.example.smartBiz.security.CustomUserPrincipal;
import com.example.smartBiz.service.DashboardService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        long unpaidCount = invoiceRepo.countByBusinessIdAndStatus(businessId, InvoiceStatus.UNPAID);
        long lowStockCount = productRepo.countLowStockProductsByBusinessId(businessId);

        // ── Today range ──
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(23, 59, 59);

        Double todayRevRaw = invoiceRepo.sumTotalAmountByBusinessAndStatusAndDateRange(
                businessId, InvoiceStatus.PAID, todayStart, todayEnd);
        Double todayExpRaw = expenseRepo.sumExpensesByBusinessBetween(businessId, todayStart, todayEnd);

        BigDecimal todayRevenue = toBigDecimal(todayRevRaw);
        BigDecimal todayExpenses = toBigDecimal(todayExpRaw);
        BigDecimal todayProfit = todayRevenue.subtract(todayExpenses);

        // ── Yesterday range (for growth %) ──
        LocalDate yesterday = today.minusDays(1);
        LocalDateTime ydayStart = yesterday.atStartOfDay();
        LocalDateTime ydayEnd = yesterday.atTime(23, 59, 59);

        Double ydayRevRaw = invoiceRepo.sumTotalAmountByBusinessAndStatusAndDateRange(
                businessId, InvoiceStatus.PAID, ydayStart, ydayEnd);
        Double ydayExpRaw = expenseRepo.sumExpensesByBusinessBetween(businessId, ydayStart, ydayEnd);

        BigDecimal ydayRevenue = toBigDecimal(ydayRevRaw);
        BigDecimal ydayExpenses = toBigDecimal(ydayExpRaw);

        // ── Month range ──
        YearMonth ym = YearMonth.now();
        LocalDateTime monthStart = ym.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = ym.atEndOfMonth().atTime(23, 59, 59);

        Double monthRevRaw = invoiceRepo.sumTotalAmountByBusinessAndStatusAndDateRange(
                businessId, InvoiceStatus.PAID, monthStart, monthEnd);
        Double monthExpRaw = expenseRepo.sumExpensesByBusinessBetween(businessId, monthStart, monthEnd);

        BigDecimal monthRevenue = toBigDecimal(monthRevRaw);
        BigDecimal monthExpenses = toBigDecimal(monthExpRaw);
        BigDecimal monthProfit = monthRevenue.subtract(monthExpenses);

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

    // ── Helper: null-safe Double → BigDecimal ──
    private BigDecimal toBigDecimal(Double val) {
        return val == null ? BigDecimal.ZERO : BigDecimal.valueOf(val);
    }

    // ── Helper: (numerator / denominator) * 100, zero-safe ──
    private Double safePercentage(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return numerator
                .multiply(BigDecimal.valueOf(100))
                .divide(denominator, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    // ── Helper: ((current - previous) / previous) * 100, zero-safe ──
    private Double safeGrowth(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            // If there was nothing yesterday, but there is something today, show 100%; else
            // 0%
            if (current != null && current.compareTo(BigDecimal.ZERO) > 0) {
                return 100.0;
            }
            return 0.0;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP)
                .doubleValue();
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
