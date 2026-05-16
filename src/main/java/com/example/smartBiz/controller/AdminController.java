package com.example.smartBiz.controller;

import com.example.smartBiz.dto.*;
import com.example.smartBiz.entity.SystemSetting;
import com.example.smartBiz.repository.SystemSettingRepo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.smartBiz.entity.AdminLog;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/api/admin")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Admin - Dashboard", description = "Admin stats overview, logs & expiring subscriptions")
public class AdminController {

    private final AdminService adminService;
    private final SystemSettingRepo systemSettingRepo;

    @Autowired
    public AdminController(AdminService adminService, SystemSettingRepo systemSettingRepo) {
        this.adminService = adminService;
        this.systemSettingRepo = systemSettingRepo;
    }

    /**
     * Authority Check Note:
     * - If using roles WITH "ROLE_" prefix in DB: @PreAuthorize("hasRole('ADMIN')")
     * - If using authority WITHOUT "ROLE_"
     * prefix: @PreAuthorize("hasAuthority('ADMIN')")
     */

    @Operation(summary = "Dashboard overview stats", description = "Returns total businesses, users, active subscriptions, and revenue.")
    @ApiResponse(responseCode = "200", description = "Stats returned")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stats/overview")
    public AdminStatsOverviewDTO getStatsOverview() {
        return adminService.getStatsOverview();
    }

    @Operation(summary = "Get latest admin logs", description = "Returns the most recent admin action logs.")
    @ApiResponse(responseCode = "200", description = "Logs returned")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/logs")
    public List<AdminLog> getAdminLogs(@Parameter(description = "Max number of logs to return") @RequestParam(name = "limit", defaultValue = "20") int limit) {
        return adminService.getAdminLogs(limit);
    }

    @Operation(summary = "Get expiring subscriptions", description = "Returns subscriptions expiring within the given number of days.")
    @ApiResponse(responseCode = "200", description = "Expiring subscriptions returned")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/subscriptions/expiring")
    public List<ExpiringSubscriptionDTO> getExpiringSubscriptions(
            @Parameter(description = "Number of days to look ahead") @RequestParam(name = "days", defaultValue = "7") int days) {
        return adminService.getExpiringSubscriptions(days);
    }

    @Operation(summary = "List all businesses", description = "Returns all registered businesses with active/disabled status.")
    @ApiResponse(responseCode = "200", description = "Businesses returned")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/businesses")
    public List<BusinessAdminDto> getAllBusinesses() {
        return adminService.getAllBusinesses();
    }

    @Operation(summary = "List all users", description = "Returns all users across all businesses.")
    @ApiResponse(responseCode = "200", description = "Users returned")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public List<UserAdminDto> getAllUsers() {
        return adminService.getAllUsers();
    }

    @Operation(summary = "Disable a business", description = "Soft-disables a business. Its users will be blocked from accessing protected endpoints.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Business disabled"),
            @ApiResponse(responseCode = "404", description = "Business not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/businesses/{businessId}/disable")
    public BusinessAdminDto disable(@Parameter(description = "Business ID") @PathVariable(name = "businessId") Long businessId) {
        adminService.logAction("INFO", "Disabled business ID: " + businessId);
        BusinessAdminDto businessAdminDto = adminService.disableBusiness(businessId);
        if (businessAdminDto == null) {
            throw new ResourceNotFoundException("Business Not Found: " + businessId);
        }
        return businessAdminDto;
    }

    @Operation(summary = "Enable a business", description = "Re-enables a previously disabled business.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Business enabled"),
            @ApiResponse(responseCode = "404", description = "Business not found")
    })
    @PatchMapping("/businesses/{businessId}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public BusinessAdminDto enable(@Parameter(description = "Business ID") @PathVariable(name = "businessId") Long businessId) {
        adminService.logAction("INFO", "Enabled business ID: " + businessId);
        BusinessAdminDto businessAdminDto = adminService.enableBusiness(businessId);
        if (businessAdminDto == null) {
            throw new ResourceNotFoundException("Business Not Found: " + businessId);
        }
        return businessAdminDto;
    }

    @Operation(summary = "Get current OpenAI API key (masked)", description = "Returns the active OpenAI API key with all but last 4 characters masked.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/settings/openai-key")
    public ResponseEntity<Map<String, String>> getOpenAiKey() {
        String key = systemSettingRepo.findByKey("openai.api.key")
                .map(SystemSetting::getValue)
                .orElse(null);
        String masked = (key != null && key.length() > 4)
                ? "sk-..." + key.substring(key.length() - 4)
                : (key != null ? "****" : "(using application.properties default)");
        return ResponseEntity.ok(Map.of("maskedKey", masked, "hasDbKey", String.valueOf(key != null && !key.isBlank())));
    }

    @Operation(summary = "Update OpenAI API key", description = "Saves a new OpenAI API key to the database. Takes effect immediately.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/settings/openai-key")
    public ResponseEntity<Map<String, String>> updateOpenAiKey(@RequestBody Map<String, String> body) {
        String newKey = body.get("apiKey");
        if (newKey == null || newKey.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "apiKey must not be empty"));
        }
        SystemSetting setting = systemSettingRepo.findByKey("openai.api.key")
                .orElse(SystemSetting.builder().key("openai.api.key").build());
        setting.setValue(newKey.trim());
        systemSettingRepo.save(setting);
        adminService.logAction("INFO", "OpenAI API key updated by admin");
        return ResponseEntity.ok(Map.of("message", "API key updated successfully"));
    }
}
