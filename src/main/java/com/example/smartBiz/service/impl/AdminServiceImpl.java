package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.UserAdminDto;
import com.example.smartBiz.dto.*;
import com.example.smartBiz.entity.*;
import com.example.smartBiz.repository.*;
import com.example.smartBiz.service.AdminService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {

    private final BusinessRepo businessRepo;
    private final UserRepo userRepo;
    private final InvoiceRepo invoiceRepo;
    private final SubscriptionRepo subscriptionRepo;
    private final AdminLogRepository adminLogRepository;

    public AdminServiceImpl(BusinessRepo businessRepo, UserRepo userRepo,
            InvoiceRepo invoiceRepo, SubscriptionRepo subscriptionRepo,
            AdminLogRepository adminLogRepository) {
        this.businessRepo = businessRepo;
        this.userRepo = userRepo;
        this.invoiceRepo = invoiceRepo;
        this.subscriptionRepo = subscriptionRepo;
        this.adminLogRepository = adminLogRepository;
    }

    @Override
    public AdminStatsOverviewDTO getStatsOverview() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
                .withNano(0);

        return AdminStatsOverviewDTO.builder()
                .totalBusinesses(businessRepo.count())
                .activeBusinesses(businessRepo.countActiveBusinesses())
                .totalUsers(userRepo.count())
                .invoicesThisMonth(invoiceRepo.countByInvoiceDateBetween(startOfMonth, now))
                .paidRevenueThisMonth(invoiceRepo.sumTotalByStatusBetween(com.example.smartBiz.enums.InvoiceStatus.PAID,
                        startOfMonth, now))
                .paidBusinesses(subscriptionRepo.countActivePaidSubscriptions())
                .freeBusinesses(subscriptionRepo.countActiveFreeSubscriptions())
                .expiringSoon(subscriptionRepo.countExpiringSoon(now, now.plusDays(7)))
                .build();
    }

    @Override
    public List<AdminLog> getAdminLogs(int limit) {
        return adminLogRepository
                .findAllByOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(0, limit)).getContent();
    }

    @Override
    public List<ExpiringSubscriptionDTO> getExpiringSubscriptions(int days) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return subscriptionRepo.findExpiringSubscriptions(now, now.plusDays(days));
    }

    @Override
    public void logAction(String level, String message) {
        AdminLog log = new AdminLog();
        log.setLevel(level);
        log.setMessage(message);
        adminLogRepository.save(log);
    }

    @Override
    public List<BusinessAdminDto> getAllBusinesses() {
        return businessRepo.findAll().stream()
                .map(b -> new BusinessAdminDto(b.getId(), b.getName(), b.getActive()))
                .toList();
    }

    @Override
    public List<UserAdminDto> getAllUsers() {
        return userRepo.findAll().stream()
                .map(u -> new UserAdminDto(
                        u.getId(),
                        u.getName(),
                        u.getEmail(),
                        u.getRole().name(),
                        u.getBusiness() != null ? u.getBusiness().getId() : null))
                .toList();
    }

    @Override
    public BusinessAdminDto disableBusiness(Long businessId) {
        Business business = businessRepo.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Business not found: " + businessId));

        business.setActive(false);
        Business saved = businessRepo.save(business);

        return new BusinessAdminDto(saved.getId(), saved.getName(), saved.getActive());
    }

    @Override
    public BusinessAdminDto enableBusiness(Long businessId) {
        Business business = businessRepo.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Business not found: " + businessId));

        business.setActive(true);
        Business saved = businessRepo.save(business);

        return new BusinessAdminDto(saved.getId(), saved.getName(), saved.getActive());
    }
}
