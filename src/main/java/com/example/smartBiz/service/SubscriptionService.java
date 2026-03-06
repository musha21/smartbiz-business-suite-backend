package com.example.smartBiz.service;

import com.example.smartBiz.dto.AssignPlanRequestDto;
import com.example.smartBiz.dto.MySubscriptionDto;
import com.example.smartBiz.entity.Subscription;

public interface SubscriptionService {

    /** Admin assigns a plan to a business */
    Subscription assignPlan(AssignPlanRequestDto dto);

    /** Owner fetches their current subscription + limits + usage */
    MySubscriptionDto getMySubscription(Long businessId);

    /** Mark subscription EXPIRED if endAt has passed */
    void refreshExpiryIfNeeded(Long businessId);

    com.example.smartBiz.dto.MyPlanDto getOwnerPlan(Long businessId);

    Subscription getBusinessSubscription(Long businessId);
}
