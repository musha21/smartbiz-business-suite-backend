package com.example.smartBiz.controller;

import com.example.smartBiz.dto.*;
import com.example.smartBiz.entity.Business;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.BusinessRepo;
import com.example.smartBiz.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/api/admin")
@CrossOrigin
public class AdminController {

    private final AdminService adminService;

@Autowired
    public AdminController(AdminService adminService, BusinessRepo businessRepo) {
        this.adminService = adminService;

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
    public BusinessAdminDto disable(@PathVariable Long businessId) {
        BusinessAdminDto businessAdminDto = adminService.disableBusiness(businessId);
        if (businessAdminDto == null) {
            throw new ResourceNotFoundException("Business Not Found: " + businessId);
        }
        return businessAdminDto;
    }

    @PatchMapping("/businesses/{businessId}/enable")
     @PreAuthorize("hasRole('ADMIN')")
    public BusinessAdminDto enable(@PathVariable Long businessId) {
        BusinessAdminDto businessAdminDto = adminService.enableBusiness(businessId);
        if (businessAdminDto == null) {
            throw new ResourceNotFoundException("Business Not Found: " + businessId);

        }
        return businessAdminDto;
    }
}
