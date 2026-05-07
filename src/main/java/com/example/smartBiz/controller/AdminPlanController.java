package com.example.smartBiz.controller;

import com.example.smartBiz.dto.PlanCreateDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.smartBiz.dto.PlanResponseDto;
import com.example.smartBiz.dto.PlanStatusUpdateDto;
import com.example.smartBiz.service.PlanService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/api/admin/plans")
@CrossOrigin
@Tag(name = "Admin - Plans", description = "CRUD for subscription plans (admin only)")
public class AdminPlanController {

    private final PlanService planService;

    public AdminPlanController(PlanService planService) {
        this.planService = planService;
    }

    @Operation(summary = "Create a new plan", description = "Creates a subscription plan with name, price, duration, and feature flags.")
    @ApiResponse(responseCode = "200", description = "Plan created")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<PlanResponseDto> create(@Valid @RequestBody PlanCreateDto dto) {
        return ResponseEntity.ok(planService.createPlan(dto));
    }

    @Operation(summary = "Update an existing plan", description = "Updates a plan's details by ID.")
    @ApiResponse(responseCode = "200", description = "Plan updated")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{planId}")
    public ResponseEntity<PlanResponseDto> update(
            @Parameter(description = "Plan ID") @PathVariable(name = "planId") Long planId,
            @Valid @RequestBody PlanCreateDto dto) {
        return ResponseEntity.ok(planService.updatePlan(planId, dto));
    }

    @Operation(summary = "List all plans", description = "Returns all plans regardless of status.")
    @ApiResponse(responseCode = "200", description = "Plans returned")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<PlanResponseDto>> getAll() {
        return ResponseEntity.ok(planService.getAllPlans());
    }

    @Operation(summary = "Update plan status", description = "Activate or deactivate a plan.")
    @ApiResponse(responseCode = "200", description = "Status updated")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{planId}/status")
    public ResponseEntity<PlanResponseDto> updateStatus(
            @Parameter(description = "Plan ID") @PathVariable(name = "planId") Long planId,
            @Valid @RequestBody PlanStatusUpdateDto dto) {
        return ResponseEntity.ok(planService.updatePlanStatus(planId, dto.getStatus()));
    }
}
