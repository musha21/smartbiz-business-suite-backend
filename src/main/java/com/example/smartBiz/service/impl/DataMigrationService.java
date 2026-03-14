package com.example.smartBiz.service.impl;

import com.example.smartBiz.entity.Business;
import com.example.smartBiz.entity.Plan;
import com.example.smartBiz.repository.BusinessRepo;
import com.example.smartBiz.repository.PlanRepo;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataMigrationService {

    private final BusinessRepo businessRepo;
    private final PlanRepo planRepo;

    @PostConstruct
    public void init() {
        migrateBusinessPlans();
    }

    @Transactional
    public void migrateBusinessPlans() {
        log.info("Checking for businesses with missing plan names...");
        List<Business> businessesToUpdate = businessRepo.findByPlanIsNull();
        
        if (businessesToUpdate.isEmpty()) {
            log.info("No businesses found with missing plan names.");
            return;
        }

        int count = 0;
        for (Business business : businessesToUpdate) {
            if (business.getPlanId() != null) {
                Optional<Plan> planOpt = planRepo.findById(business.getPlanId());
                if (planOpt.isPresent()) {
                    business.setPlan(planOpt.get().getName().trim());
                    businessRepo.save(business);
                    count++;
                }
            }
        }

        log.info("Successfully migrated {} businesses with plan details.", count);
    }
}
