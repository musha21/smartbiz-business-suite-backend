package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.DashboardSummaryDto;
import com.example.smartBiz.enums.InvoiceStatus;
import com.example.smartBiz.repository.ExpenseRepo;
import com.example.smartBiz.repository.InvoiceRepo;
import com.example.smartBiz.repository.ProductRepo;
import com.example.smartBiz.security.RequestContext;
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
    private final RequestContext requestContext;

    public DashboardServiceImpl(
            InvoiceRepo invoiceRepo,
            ProductRepo productRepo,
            ExpenseRepo expenseRepo,
            RequestContext requestContext
    ) {
        this.invoiceRepo = invoiceRepo;
        this.productRepo = productRepo;
        this.expenseRepo = expenseRepo;
        this.requestContext = requestContext;
    }

    private Long requireBusinessId() {
        Long businessId = requestContext.getBusinessId();
        if (businessId == null) {
            throw new RuntimeException("Business context missing (JWT required)");
        }
        return businessId;
    }

    @Override
    public DashboardSummaryDto getSummary() {

        Long businessId = requireBusinessId();

        // counts (per business)
        long unpaidCount = invoiceRepo.countByBusinessIdAndStatus(businessId, InvoiceStatus.UNPAID);
        long lowStockCount = productRepo.countLowStockProductsByBusinessId(businessId);

        // today range
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(23, 59, 59);

        double todayRevenue = invoiceRepo.sumTotalAmountByBusinessAndStatusAndDateRange(
                businessId, InvoiceStatus.PAID, todayStart, todayEnd
        );

        double todayExpenses = expenseRepo.sumExpensesByBusinessBetween(businessId, todayStart, todayEnd);
        double todayProfit = todayRevenue - todayExpenses;

        // month range
        YearMonth ym = YearMonth.now();
        LocalDateTime monthStart = ym.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = ym.atEndOfMonth().atTime(23, 59, 59);

        double monthRevenue = invoiceRepo.sumTotalAmountByBusinessAndStatusAndDateRange(
                businessId, InvoiceStatus.PAID, monthStart, monthEnd
        );

        double monthExpenses = expenseRepo.sumExpensesByBusinessBetween(businessId, monthStart, monthEnd);
        double monthProfit = monthRevenue - monthExpenses;

        return new DashboardSummaryDto(
                todayRevenue, todayExpenses, todayProfit,
                monthRevenue, monthExpenses, monthProfit,
                unpaidCount, lowStockCount
        );
    }
}
