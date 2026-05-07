package com.example.smartBiz.controller;

import com.example.smartBiz.dto.DashboardSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.smartBiz.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/api/dashboard")
@CrossOrigin
@Tag(name = "Dashboard", description = "Business owner dashboard summary")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "Get dashboard summary", description = "Returns key business metrics: total revenue, total expenses, invoice counts, top products, unpaid invoices, and monthly revenue trend.")
    @ApiResponse(responseCode = "200", description = "Dashboard summary returned")
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDto> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }
}
