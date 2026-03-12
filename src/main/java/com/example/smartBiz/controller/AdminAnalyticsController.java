package com.example.smartBiz.controller;

import com.example.smartBiz.dto.*;
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

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/plan-analytics")
    public ResponseEntity<PlanAnalyticsDto> getPlanAnalytics() {
        logAction("VIEW_PLAN_ANALYTICS");
        return ResponseEntity.ok(analyticsService.getPlanAnalytics());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/weekly-activity")
    public ResponseEntity<List<WeeklyActivityDto>> getWeeklyActivity() {
        logAction("VIEW_WEEKLY_ACTIVITY");
        return ResponseEntity.ok(analyticsService.getWeeklyActivity());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stats")
    public ResponseEntity<SystemStatsDto> getSystemStats() {
        logAction("VIEW_SYSTEM_STATS");
        return ResponseEntity.ok(analyticsService.getSystemStats());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/ai-analytics")
    public ResponseEntity<AiAnalyticsDto> getAiAnalytics() {
        logAction("VIEW_AI_ANALYTICS");
        return ResponseEntity.ok(analyticsService.getAiAnalytics());
    }
}
