package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.MonthlyRevenueDto;
import com.example.smartBiz.dto.TopProductDto;
import com.example.smartBiz.dto.UnpaidInvoiceDto;
import com.example.smartBiz.enums.InvoiceStatus;
import com.example.smartBiz.repository.InvoiceItemRepo;
import com.example.smartBiz.repository.InvoiceRepo;
import com.example.smartBiz.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    private final InvoiceRepo invoiceRepository;
    private final InvoiceItemRepo invoiceItemRepository;

    @Autowired
    public ReportServiceImpl(InvoiceRepo invoiceRepository,
                             InvoiceItemRepo invoiceItemRepository) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
    }

    @Override
    public MonthlyRevenueDto getMonthlyRevenue(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.atEndOfMonth().atTime(23, 59, 59);

        double revenue = invoiceRepository.sumTotalByStatusBetween(InvoiceStatus.PAID, start, end);
        return new MonthlyRevenueDto(year, month, revenue);
    }

    @Override
    public List<TopProductDto> getTopProducts(int year, int month, int limit) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.atEndOfMonth().atTime(23, 59, 59);

        return invoiceItemRepository.findTopProducts(
                InvoiceStatus.PAID, start, end,
                PageRequest.of(0, limit)
        );
    }

    @Override
    public List<UnpaidInvoiceDto> getUnpaidInvoices() {
        return invoiceRepository.findInvoicesByStatusAsDto(InvoiceStatus.UNPAID);
    }
}
