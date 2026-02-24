package com.example.smartBiz.controller;

import com.example.smartBiz.dto.PlanLimitUpsertDto;
import com.example.smartBiz.service.PlanLimitService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/api/admin/plans/{planId}/limits")
@CrossOrigin
public class AdminPlanLimitController {

    private final PlanLimitService planLimitService;

    public AdminPlanLimitController(PlanLimitService planLimitService) {
        this.planLimitService = planLimitService;
    }

    /** Upsert (create/update) limits for a plan */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public ResponseEntity<String> upsert(
            @PathVariable(name = "planId") Long planId,
            @Valid @RequestBody List<PlanLimitUpsertDto> limits) {
        planLimitService.upsertLimits(planId, limits);
        return ResponseEntity.ok("Limits updated for plan " + planId);
    }

    /** Get all limits for a plan */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Map<String, Long>> getLimits(@PathVariable(name = "planId") Long planId) {
        return ResponseEntity.ok(planLimitService.getLimitsMap(planId));
    }
}
