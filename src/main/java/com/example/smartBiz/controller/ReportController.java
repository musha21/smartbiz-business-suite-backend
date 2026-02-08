package com.example.smartBiz.controller;

import com.example.smartBiz.dto.MonthlyRevenueDto;
import com.example.smartBiz.dto.TopProductDto;
import com.example.smartBiz.dto.UnpaidInvoiceDto;
import com.example.smartBiz.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/api/reports")
@CrossOrigin
public class ReportController {

    private final ReportService reportService;
@Autowired
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // ✅ Monthly revenue (PAID)
    // GET /v1/api/reports/revenue/monthly?year=2026&month=2
    @GetMapping("/revenue/monthly")
    public ResponseEntity<MonthlyRevenueDto> monthlyRevenue(
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ResponseEntity.ok(reportService.getMonthlyRevenue(year, month));
    }

    // ✅ Top 5 selling products (by qty)
    // GET /v1/api/reports/products/top?year=2026&month=2&limit=5
    @GetMapping("/products/top")
    public ResponseEntity<List<TopProductDto>> topProducts(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ResponseEntity.ok(reportService.getTopProducts(year, month, limit));
    }

    // ✅ Unpaid invoices list
    // GET /v1/api/reports/invoices/unpaid
    @GetMapping("/invoices/unpaid")
    public ResponseEntity<List<UnpaidInvoiceDto>> unpaidInvoices() {
        return ResponseEntity.ok(reportService.getUnpaidInvoices());
    }
}
