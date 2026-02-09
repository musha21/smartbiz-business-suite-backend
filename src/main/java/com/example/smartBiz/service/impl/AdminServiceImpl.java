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

    public AdminServiceImpl(BusinessRepo businessRepo, UserRepo userRepo) {
        this.businessRepo = businessRepo;
        this.userRepo = userRepo;
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
                        u.getBusiness() != null ? u.getBusiness().getId() : null
                ))
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
