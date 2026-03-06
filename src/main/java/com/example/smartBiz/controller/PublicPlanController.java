package com.example.smartBiz.controller;

import com.example.smartBiz.dto.PlanResponseDto;
import com.example.smartBiz.service.PlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/api/plans")
@CrossOrigin
public class PublicPlanController {

    private final PlanService planService;

    public PublicPlanController(PlanService planService) {
        this.planService = planService;
    }

    /**
     * List only active plans.
     * Accessible to both OWNERS and ADMINS.
     */
    @GetMapping("/active")
    public ResponseEntity<List<com.example.smartBiz.dto.PlanCardDto>> getActive() {
        return ResponseEntity.ok(planService.getActivePlanCards());
    }
}
