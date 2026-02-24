package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.PlanCreateDto;
import com.example.smartBiz.dto.PlanResponseDto;
import com.example.smartBiz.dto.PlanStatusUpdateDto;
import com.example.smartBiz.entity.Plan;
import com.example.smartBiz.enums.PlanStatus;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.PlanRepo;
import com.example.smartBiz.service.PlanLimitService;
import com.example.smartBiz.service.PlanService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlanServiceImpl implements PlanService {

    private final PlanRepo planRepo;
    private final PlanLimitService planLimitService;

    public PlanServiceImpl(PlanRepo planRepo, PlanLimitService planLimitService) {
        this.planRepo = planRepo;
        this.planLimitService = planLimitService;
    }

    @Override
    @Transactional
    public PlanResponseDto createPlan(PlanCreateDto dto) {
        // Ensure unique code
        if (planRepo.findByCode(dto.getCode().toUpperCase()).isPresent()) {
            throw new RuntimeException("Plan code already exists: " + dto.getCode());
        }

        Plan plan = new Plan();
        plan.setCode(dto.getCode().toUpperCase());
        plan.setName(dto.getName());
        plan.setDescription(dto.getDescription());
        plan.setMonthlyPrice(dto.getMonthlyPrice());
        plan.setYearlyPrice(dto.getYearlyPrice());
        plan.setActive(true);
        plan.setStatus(PlanStatus.ACTIVE);

        Plan saved = planRepo.save(plan);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public PlanResponseDto updatePlan(Long planId, PlanCreateDto dto) {
        Plan plan = planRepo.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + planId));

        plan.setCode(dto.getCode().toUpperCase());
        plan.setName(dto.getName());
        plan.setDescription(dto.getDescription());
        plan.setMonthlyPrice(dto.getMonthlyPrice());
        plan.setYearlyPrice(dto.getYearlyPrice());

        Plan saved = planRepo.save(plan);
        return mapToResponse(saved);
    }

    @Override
    public List<PlanResponseDto> getAllPlans() {
        return planRepo.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PlanResponseDto> getActivePlans() {
        return planRepo.findAllByStatus(PlanStatus.ACTIVE).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PlanResponseDto updatePlanStatus(Long planId, String status) {
        Plan plan = planRepo.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + planId));

        try {
            PlanStatus newStatus = PlanStatus.valueOf(status.toUpperCase());
            plan.setStatus(newStatus);
            // Sync active for backward compatibility if needed, but requirements say use
            // status
            plan.setActive(newStatus == PlanStatus.ACTIVE);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + status + ". Use ACTIVE or INACTIVE");
        }

        Plan saved = planRepo.save(plan);
        return mapToResponse(saved);
    }

    private PlanResponseDto mapToResponse(Plan plan) {
        PlanResponseDto dto = new PlanResponseDto();
        dto.setId(plan.getId());
        dto.setCode(plan.getCode());
        dto.setName(plan.getName());
        dto.setDescription(plan.getDescription());
        dto.setActive(plan.getActive());
        dto.setStatus(plan.getStatus().name());
        dto.setMonthlyPrice(plan.getMonthlyPrice());
        dto.setYearlyPrice(plan.getYearlyPrice());
        dto.setCreatedAt(plan.getCreatedAt());
        // Attach limits map
        dto.setLimits(planLimitService.getLimitsMap(plan.getId()));
        return dto;
    }
}
