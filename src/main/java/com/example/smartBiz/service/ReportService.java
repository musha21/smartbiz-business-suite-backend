package com.example.smartBiz.service;

import com.example.smartBiz.dto.MonthlyRevenueDto;
import com.example.smartBiz.dto.TopProductDto;
import com.example.smartBiz.dto.UnpaidInvoiceDto;

import java.util.List;

public interface ReportService {
    MonthlyRevenueDto getMonthlyRevenue(int year, int month);
    List<TopProductDto> getTopProducts(int year, int month, int limit);
    List<UnpaidInvoiceDto> getUnpaidInvoices(int year, int month, int limit);
}
