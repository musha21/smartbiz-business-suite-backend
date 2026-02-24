package com.example.smartBiz.controller;

import com.example.smartBiz.dto.PlanCreateDto;
import com.example.smartBiz.dto.PlanResponseDto;
import com.example.smartBiz.service.PlanService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/api/admin/plans")
@CrossOrigin
public class AdminPlanController {

    private final PlanService planService;

    public AdminPlanController(PlanService planService) {
        this.planService = planService;
    }

    /** Create a new plan (admin only) */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<PlanResponseDto> create(@Valid @RequestBody PlanCreateDto dto) {
        return ResponseEntity.ok(planService.createPlan(dto));
    }

    /** Update an existing plan (admin only) */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{planId}")
    public ResponseEntity<PlanResponseDto> update(
            @PathVariable(name = "planId") Long planId,
            @Valid @RequestBody PlanCreateDto dto) {
        return ResponseEntity.ok(planService.updatePlan(planId, dto));
    }

    /** List all plans (admin sees all, including inactive) */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<PlanResponseDto>> getAll() {
        return ResponseEntity.ok(planService.getAllPlans());
    }
}
