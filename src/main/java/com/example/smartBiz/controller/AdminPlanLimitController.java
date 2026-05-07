package com.example.smartBiz.controller;

import com.example.smartBiz.dto.PlanLimitUpsertDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin - Plan Limits", description = "Manage limits per plan (admin only)")
public class AdminPlanLimitController {

    private final PlanLimitService planLimitService;

    public AdminPlanLimitController(PlanLimitService planLimitService) {
        this.planLimitService = planLimitService;
    }

    @Operation(summary = "Upsert plan limits", description = "Create or update resource limits (e.g. INVOICES_PER_MONTH, MAX_CUSTOMERS, AI_CREDITS) for a plan.")
    @ApiResponse(responseCode = "200", description = "Limits updated")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public ResponseEntity<String> upsert(
            @Parameter(description = "Plan ID") @PathVariable(name = "planId") Long planId,
            @Valid @RequestBody List<PlanLimitUpsertDto> limits) {
        planLimitService.upsertLimits(planId, limits);
        return ResponseEntity.ok("Limits updated for plan " + planId);
    }

    @Operation(summary = "Get plan limits", description = "Returns all limits configured for the given plan as a key-value map.")
    @ApiResponse(responseCode = "200", description = "Limits returned")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Map<String, Long>> getLimits(@Parameter(description = "Plan ID") @PathVariable(name = "planId") Long planId) {
        return ResponseEntity.ok(planLimitService.getLimitsMap(planId));
    }
}
