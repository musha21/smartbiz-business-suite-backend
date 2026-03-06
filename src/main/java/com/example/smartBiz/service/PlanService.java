package com.example.smartBiz.service;

import com.example.smartBiz.dto.PlanCreateDto;
import com.example.smartBiz.dto.PlanResponseDto;

import java.util.List;

public interface PlanService {

    PlanResponseDto createPlan(PlanCreateDto dto);

    PlanResponseDto updatePlan(Long planId, PlanCreateDto dto);

    List<PlanResponseDto> getAllPlans();

    List<PlanResponseDto> getActivePlans();

    List<com.example.smartBiz.dto.PlanCardDto> getActivePlanCards();

    PlanResponseDto updatePlanStatus(Long planId, String status);
}
