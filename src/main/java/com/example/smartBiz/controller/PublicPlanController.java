package com.example.smartBiz.controller;

import com.example.smartBiz.dto.PlanResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Public Plans", description = "Publicly visible active plans")
public class PublicPlanController {

    private final PlanService planService;

    public PublicPlanController(PlanService planService) {
        this.planService = planService;
    }

    @Operation(summary = "List active plans", description = "Returns all currently active subscription plans with pricing and feature details. Accessible to authenticated OWNERS and ADMINS.")
    @ApiResponse(responseCode = "200", description = "Active plans returned")
    @GetMapping("/active")
    public ResponseEntity<List<com.example.smartBiz.dto.PlanCardDto>> getActive() {
        return ResponseEntity.ok(planService.getActivePlanCards());
    }
}
