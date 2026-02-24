package com.example.smartBiz.service;

import com.example.smartBiz.dto.BusinessAdminDto;
import com.example.smartBiz.dto.UserAdminDto;

import java.util.List;

public interface AdminService {
    List<BusinessAdminDto> getAllBusinesses();

    List<UserAdminDto> getAllUsers();

    BusinessAdminDto disableBusiness(Long businessId);

    BusinessAdminDto enableBusiness(Long businessId);

    com.example.smartBiz.dto.AdminStatsOverviewDTO getStatsOverview();

    java.util.List<com.example.smartBiz.entity.AdminLog> getAdminLogs(int limit);

    java.util.List<com.example.smartBiz.dto.ExpiringSubscriptionDTO> getExpiringSubscriptions(int days);

    void logAction(String level, String message);
}
