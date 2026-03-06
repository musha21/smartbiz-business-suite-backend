package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.MonthlyRevenueDto;
import com.example.smartBiz.dto.TopProductDto;
import com.example.smartBiz.dto.UnpaidInvoiceDto;
import com.example.smartBiz.enums.InvoiceStatus;
import com.example.smartBiz.repository.InvoiceItemRepo;
import com.example.smartBiz.repository.InvoiceRepo;
import com.example.smartBiz.security.CustomUserPrincipal;
import com.example.smartBiz.service.ReportService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    private final InvoiceRepo invoiceRepository;
    private final InvoiceItemRepo invoiceItemRepository;

    public ReportServiceImpl(InvoiceRepo invoiceRepository,
            InvoiceItemRepo invoiceItemRepository) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
    }

    private Long requireBusinessId() {
        CustomUserPrincipal principal = CustomUserPrincipal.getCurrent();
        if (principal == null || principal.getBusinessId() == null) {
            throw new RuntimeException("Business context missing (JWT required)");
        }
        return principal.getBusinessId();
    }

    @Override
    public MonthlyRevenueDto getMonthlyRevenue(int year, int month) {
        Long businessId = requireBusinessId();

        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.atEndOfMonth().atTime(23, 59, 59);

        double revenue = invoiceRepository.sumTotalByStatusBetweenAndBusinessId(
                InvoiceStatus.PAID, start, end, businessId);

        // ✅ matches DTO: (year, month, paidRevenue)
        return new MonthlyRevenueDto(year, month, revenue);
    }

    @Override
    public List<TopProductDto> getTopProducts(int year, int month, int limit) {
        Long businessId = requireBusinessId();

        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.atEndOfMonth().atTime(23, 59, 59);

        return invoiceItemRepository.findTopProductsByBusiness(
                InvoiceStatus.PAID, start, end, businessId,
                PageRequest.of(0, limit));
    }

    @Override
    public List<UnpaidInvoiceDto> getUnpaidInvoices(int year, int month, int limit) {
        Long businessId = requireBusinessId();

        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.atEndOfMonth().atTime(23, 59, 59);

        return invoiceRepository.findInvoicesByStatusAsDtoAndBusinessId(
                InvoiceStatus.UNPAID,
                start,
                end,
                businessId,
                PageRequest.of(0, limit));
    }
}
