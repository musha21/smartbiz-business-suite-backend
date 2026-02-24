package com.example.smartBiz.controller;

import com.example.smartBiz.dto.MySubscriptionDto;
import com.example.smartBiz.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/api/subscriptions")
@CrossOrigin
public class OwnerSubscriptionController {

    private final SubscriptionService subscriptionService;

    public OwnerSubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    /**
     * Owner fetches their current subscription + plan limits + usage.
     * GET /v1/api/subscriptions/my?businessId=1
     */
    @GetMapping("/my")
    public ResponseEntity<MySubscriptionDto> mySubscription(@RequestParam(name = "businessId") Long businessId) {
        return ResponseEntity.ok(subscriptionService.getMySubscription(businessId));
    }
}
