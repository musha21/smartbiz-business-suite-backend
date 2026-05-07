package com.example.smartBiz.controller;

import com.example.smartBiz.dto.CancelSubscriptionResponseDto;
import com.example.smartBiz.dto.MySubscriptionDto;
import com.example.smartBiz.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.smartBiz.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/api/subscriptions")
@CrossOrigin
@Tag(name = "Owner Subscription", description = "Owner's subscription details and management")
public class OwnerSubscriptionController {

    private final SubscriptionService subscriptionService;

    public OwnerSubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Operation(summary = "Get my subscription",
            description = "Returns the owner's current subscription details including plan, limits, usage, and expiry date.")
    @ApiResponse(responseCode = "200", description = "Subscription returned")
    @GetMapping("/my")
    public ResponseEntity<MySubscriptionDto> mySubscription() {
        CustomUserPrincipal principal = CustomUserPrincipal.getCurrent();
        if (principal == null || principal.getBusinessId() == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(subscriptionService.getMySubscription(principal.getBusinessId()));
    }

    @Operation(summary = "Cancel my subscription",
            description = "Cancels the owner's active subscription. " +
                    "Access continues until the current billing period ends (grace period). " +
                    "Free plans cannot be canceled.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cancellation processed"),
            @ApiResponse(responseCode = "404", description = "No active subscription found"),
            @ApiResponse(responseCode = "400", description = "Free plan cannot be canceled"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping("/cancel")
    public ResponseEntity<CancelSubscriptionResponseDto> cancelSubscription() {
        CustomUserPrincipal principal = CustomUserPrincipal.getCurrent();
        if (principal == null || principal.getBusinessId() == null) {
            return ResponseEntity.status(401).build();
        }
        CancelSubscriptionResponseDto response =
                subscriptionService.cancelSubscription(principal.getBusinessId());
        return ResponseEntity.ok(response);
    }
}

