package com.example.smartBiz.controller;

import com.example.smartBiz.dto.AssignPlanRequestDto;
import com.example.smartBiz.entity.Subscription;
import com.example.smartBiz.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/api/admin/subscriptions")
@CrossOrigin
public class AdminSubscriptionController {

    private final SubscriptionService subscriptionService;

    public AdminSubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    /** Assign a plan to a business (admin only) */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/assign")
    public ResponseEntity<String> assignPlan(@Valid @RequestBody AssignPlanRequestDto dto) {
        Subscription sub = subscriptionService.assignPlan(dto);
        return ResponseEntity.ok("Plan assigned. Subscription ID: " + sub.getId()
                + " | Status: " + sub.getStatus()
                + " | Ends: " + (sub.getEndAt() != null ? sub.getEndAt() : "NEVER (free)"));
    }
}
