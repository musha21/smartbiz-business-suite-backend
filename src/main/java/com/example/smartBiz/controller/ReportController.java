package com.example.smartBiz.controller;

import com.example.smartBiz.dto.AnalyticsResponseDto;
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
public class ReportController {

    private final AiService aiService;

    public ReportController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<AnalyticsResponseDto> getAnalytics(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to")   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        if (principal == null || principal.getBusinessId() == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(aiService.getAnalytics(principal.getBusinessId(), from, to));
    }
}