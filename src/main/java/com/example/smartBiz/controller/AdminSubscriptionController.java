package com.example.smartBiz.controller;

import com.example.smartBiz.dto.AssignPlanRequestDto;
import com.example.smartBiz.dto.CancelSubscriptionResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.smartBiz.entity.Subscription;
import com.example.smartBiz.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/api/admin/subscriptions")
@CrossOrigin
@Tag(name = "Admin - Subscriptions", description = "Assign and manage subscriptions for businesses (admin only)")
public class AdminSubscriptionController {

    private final SubscriptionService subscriptionService;

    public AdminSubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Operation(summary = "Assign a plan to a business", description = "Creates or updates the subscription for a business with the specified plan, duration, and payment status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plan assigned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Business or plan not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/assign")
    public ResponseEntity<String> assignPlan(@Valid @RequestBody AssignPlanRequestDto dto) {
        Subscription sub = subscriptionService.assignPlan(dto);
        return ResponseEntity.ok("Plan assigned. Subscription ID: " + sub.getId()
                + " | Status: " + sub.getStatus()
                + " | Ends: " + (sub.getEndAt() != null ? sub.getEndAt() : "NEVER (free)"));
    }

    @Operation(summary = "Cancel a business subscription",
            description = "Admin force-cancels a business's subscription. " +
                    "Use immediate=true for instant cancellation, " +
                    "or immediate=false to let it expire at the end of the billing period.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cancellation processed"),
            @ApiResponse(responseCode = "404", description = "No active subscription found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/cancel/{businessId}")
    public ResponseEntity<CancelSubscriptionResponseDto> cancelSubscription(
            @Parameter(description = "Business ID") @PathVariable Long businessId,
            @Parameter(description = "True for immediate cancel, false for end-of-period")
            @RequestParam(defaultValue = "true") boolean immediate) {
        CancelSubscriptionResponseDto response =
                subscriptionService.adminCancelSubscription(businessId, immediate);
        return ResponseEntity.ok(response);
    }
}

