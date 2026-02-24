package com.example.smartBiz.controller;

import com.example.smartBiz.dto.*;
import com.example.smartBiz.entity.AdminLog;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/api/admin")
@CrossOrigin(origins = "http://localhost:3000")
public class AdminController {

    private final AdminService adminService;

    @Autowired
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * Authority Check Note:
     * - If using roles WITH "ROLE_" prefix in DB: @PreAuthorize("hasRole('ADMIN')")
     * - If using authority WITHOUT "ROLE_"
     * prefix: @PreAuthorize("hasAuthority('ADMIN')")
     */

    // 1) Dashboard Overview Stats
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stats/overview")
    public AdminStatsOverviewDTO getStatsOverview() {
        return adminService.getStatsOverview();
    }

    // 2) Latest Admin Logs
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/logs")
    public List<AdminLog> getAdminLogs(@RequestParam(name = "limit", defaultValue = "20") int limit) {
        return adminService.getAdminLogs(limit);
    }

    // 3) Expiring Subscriptions
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/subscriptions/expiring")
    public List<ExpiringSubscriptionDTO> getExpiringSubscriptions(
            @RequestParam(name = "days", defaultValue = "7") int days) {
        return adminService.getExpiringSubscriptions(days);
    }

    // ✅ View all businesses
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/businesses")
    public List<BusinessAdminDto> getAllBusinesses() {
        return adminService.getAllBusinesses();
    }

    // ✅ View all users
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public List<UserAdminDto> getAllUsers() {
        return adminService.getAllUsers();
    }

    // ✅ Disable a business (soft delete)
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/businesses/{businessId}/disable")
    public BusinessAdminDto disable(@PathVariable(name = "businessId") Long businessId) {
        adminService.logAction("INFO", "Disabled business ID: " + businessId);
        BusinessAdminDto businessAdminDto = adminService.disableBusiness(businessId);
        if (businessAdminDto == null) {
            throw new ResourceNotFoundException("Business Not Found: " + businessId);
        }
        return businessAdminDto;
    }

    @PatchMapping("/businesses/{businessId}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public BusinessAdminDto enable(@PathVariable(name = "businessId") Long businessId) {
        adminService.logAction("INFO", "Enabled business ID: " + businessId);
        BusinessAdminDto businessAdminDto = adminService.enableBusiness(businessId);
        if (businessAdminDto == null) {
            throw new ResourceNotFoundException("Business Not Found: " + businessId);
        }
        return businessAdminDto;
    }
}
