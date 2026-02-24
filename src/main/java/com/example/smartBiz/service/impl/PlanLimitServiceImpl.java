package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.PlanLimitUpsertDto;
import com.example.smartBiz.entity.Plan;
import com.example.smartBiz.entity.PlanLimit;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.PlanLimitRepo;
import com.example.smartBiz.repository.PlanRepo;
import com.example.smartBiz.service.PlanLimitService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PlanLimitServiceImpl implements PlanLimitService {

    private final PlanLimitRepo planLimitRepo;
    private final PlanRepo planRepo;

    public PlanLimitServiceImpl(PlanLimitRepo planLimitRepo, PlanRepo planRepo) {
        this.planLimitRepo = planLimitRepo;
        this.planRepo = planRepo;
    }

    @Override
    @Transactional
    public void upsertLimits(Long planId, List<PlanLimitUpsertDto> limits) {
        Plan plan = planRepo.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + planId));

        for (PlanLimitUpsertDto dto : limits) {
            String key = dto.getKey().toUpperCase().trim();

            Optional<PlanLimit> existing = planLimitRepo.findByPlanIdAndLimitKey(planId, key);

            if (existing.isPresent()) {
                // Update existing limit
                PlanLimit limit = existing.get();
                limit.setLimitValue(dto.getValue());
                planLimitRepo.save(limit);
            } else {
                // Create new limit
                PlanLimit limit = new PlanLimit();
                limit.setPlan(plan);
                limit.setLimitKey(key);
                limit.setLimitValue(dto.getValue());
                planLimitRepo.save(limit);
            }
        }
    }

    @Override
    public Map<String, Long> getLimitsMap(Long planId) {
        List<PlanLimit> limits = planLimitRepo.findByPlanId(planId);
        Map<String, Long> map = new HashMap<>();
        for (PlanLimit l : limits) {
            map.put(l.getLimitKey(), l.getLimitValue());
        }
        return map;
    }

    @Override
    public Long getLimitValueOrDefault(Long planId, String limitKey, Long defaultValue) {
        return planLimitRepo.findByPlanIdAndLimitKey(planId, limitKey.toUpperCase().trim())
                .map(PlanLimit::getLimitValue)
                .orElse(defaultValue);
    }
}
