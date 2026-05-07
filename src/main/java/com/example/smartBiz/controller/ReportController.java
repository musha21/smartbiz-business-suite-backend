package com.example.smartBiz.controller;

import com.example.smartBiz.dto.AnalyticsResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.smartBiz.security.CustomUserPrincipal;
import com.example.smartBiz.service.AiService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/v1/api/reports")
@CrossOrigin
@Tag(name = "Reports", description = "Analytics reports with date range")
public class ReportController {

    private final AiService aiService;

    public ReportController(AiService aiService) {
        this.aiService = aiService;
    }

    @Operation(summary = "Get business analytics", description = "Returns revenue, expense, invoice, and product analytics for the given date range. Requires OWNER or ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Analytics returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<AnalyticsResponseDto> getAnalytics(
            @Parameter(description = "Start date (ISO format, e.g. 2025-01-01)") @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date (ISO format, e.g. 2025-12-31)") @RequestParam("to")   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        if (principal == null || principal.getBusinessId() == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(aiService.getAnalytics(principal.getBusinessId(), from, to));
    }
}