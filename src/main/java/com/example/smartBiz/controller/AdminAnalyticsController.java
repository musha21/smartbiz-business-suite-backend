package com.example.smartBiz.controller;

import com.example.smartBiz.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.smartBiz.entity.ActivityLog;
import com.example.smartBiz.repository.ActivityLogRepo;
import com.example.smartBiz.service.AdminAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/api/admin")
@CrossOrigin
@Tag(name = "Admin - Analytics", description = "Platform-wide analytics for admins")
public class AdminAnalyticsController {

    private final AdminAnalyticsService analyticsService;
    private final ActivityLogRepo activityLogRepo;

    public AdminAnalyticsController(AdminAnalyticsService analyticsService, ActivityLogRepo activityLogRepo) {
        this.analyticsService = analyticsService;
        this.activityLogRepo = activityLogRepo;
    }

    private void logAction(String action) {
        // In a real app, get userId from SecurityContext
        Long userId = 1L; // Placeholder for admin ID
        activityLogRepo.save(ActivityLog.builder()
                .userId(userId)
                .action(action)
                .entityType("ANALYTICS")
                .build());
    }

    @Operation(summary = "Get plan analytics", description = "Returns plan distribution and adoption metrics.")
    @ApiResponse(responseCode = "200", description = "Plan analytics returned")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/plan-analytics")
    public ResponseEntity<PlanAnalyticsDto> getPlanAnalytics() {
        logAction("VIEW_PLAN_ANALYTICS");
        return ResponseEntity.ok(analyticsService.getPlanAnalytics());
    }

    @Operation(summary = "Get weekly activity", description = "Returns user activity aggregated by day for the past week.")
    @ApiResponse(responseCode = "200", description = "Weekly activity returned")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/weekly-activity")
    public ResponseEntity<List<WeeklyActivityDto>> getWeeklyActivity() {
        logAction("VIEW_WEEKLY_ACTIVITY");
        return ResponseEntity.ok(analyticsService.getWeeklyActivity());
    }

    @Operation(summary = "Get system stats", description = "Returns platform-wide statistics: total users, businesses, invoices, revenue.")
    @ApiResponse(responseCode = "200", description = "System stats returned")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stats")
    public ResponseEntity<SystemStatsDto> getSystemStats() {
        logAction("VIEW_SYSTEM_STATS");
        return ResponseEntity.ok(analyticsService.getSystemStats());
    }

    @Operation(summary = "Get AI analytics", description = "Returns AI usage metrics: total calls, credits consumed, top users.")
    @ApiResponse(responseCode = "200", description = "AI analytics returned")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/ai-analytics")
    public ResponseEntity<AiAnalyticsDto> getAiAnalytics() {
        logAction("VIEW_AI_ANALYTICS");
        return ResponseEntity.ok(analyticsService.getAiAnalytics());
    }
}
