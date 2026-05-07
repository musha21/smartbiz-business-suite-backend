package com.example.smartBiz.controller;

import com.example.smartBiz.dto.MyPlanDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.smartBiz.dto.UsageCountersDto;
import com.example.smartBiz.entity.Subscription;
import com.example.smartBiz.repository.CustomerRepo;
import com.example.smartBiz.repository.ProductRepo;
import com.example.smartBiz.repository.ProductRepo;
import com.example.smartBiz.security.CustomUserPrincipal;
import com.example.smartBiz.service.PlanLimitService;
import com.example.smartBiz.service.SubscriptionService;
import com.example.smartBiz.service.UsageCounterService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/api/owner")
@CrossOrigin
@Tag(name = "Owner Plan", description = "Owner's current plan & usage counters")
public class OwnerPlanController {

    private final SubscriptionService subscriptionService;
    private final UsageCounterService usageCounterService;
    private final PlanLimitService planLimitService;
    private final CustomerRepo customerRepo;
    private final ProductRepo productRepo;

    public OwnerPlanController(SubscriptionService subscriptionService,
            UsageCounterService usageCounterService,
            PlanLimitService planLimitService,
            CustomerRepo customerRepo,
            ProductRepo productRepo) {
        this.subscriptionService = subscriptionService;
        this.usageCounterService = usageCounterService;
        this.planLimitService = planLimitService;
        this.customerRepo = customerRepo;
        this.productRepo = productRepo;
    }

    @Operation(summary = "Get my plan", description = "Returns the subscription plan currently assigned to the owner's business.")
    @ApiResponse(responseCode = "200", description = "Plan returned")
    @GetMapping("/my-plan")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<MyPlanDto> getMyPlan() {
        CustomUserPrincipal principal = CustomUserPrincipal.getCurrent();
        if (principal == null || principal.getBusinessId() == null) {
            return ResponseEntity.badRequest().build();
        }
        Long businessId = principal.getBusinessId();
        return ResponseEntity.ok(subscriptionService.getOwnerPlan(businessId));
    }

    @Operation(summary = "Get usage counters", description = "Returns the owner's current month usage (invoices, customers, products, AI credits) vs. plan limits. A limit of -1 means unlimited.")
    @ApiResponse(responseCode = "200", description = "Usage counters returned")
    @GetMapping("/usage")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<UsageCountersDto> getUsage() {
        CustomUserPrincipal principal = CustomUserPrincipal.getCurrent();
        if (principal == null || principal.getBusinessId() == null) {
            return ResponseEntity.status(401).build();
        }
        Long businessId = principal.getBusinessId();

        Subscription sub = subscriptionService.getBusinessSubscription(businessId);
        Long planId = (sub != null && sub.getPlan() != null) ? sub.getPlan().getId() : null;

        UsageCountersDto dto = UsageCountersDto.builder()
                .invoicesUsed(usageCounterService.getThisMonthInvoiceCount(businessId))
                .invoicesLimit(
                        planId != null ? planLimitService.getLimitValueOrDefault(planId, "INVOICES_PER_MONTH", -1L)
                                : -1L)
                .customersUsed(customerRepo.countByBusinessIdAndArchivedFalse(businessId))
                .customersLimit(
                        planId != null ? planLimitService.getLimitValueOrDefault(planId, "MAX_CUSTOMERS", -1L) : -1L)
                .productsUsed(productRepo.countByBusinessId(businessId))
                .productsLimit(
                        planId != null ? planLimitService.getLimitValueOrDefault(planId, "MAX_PRODUCTS", -1L) : -1L)
                .aiUsed(usageCounterService.getThisMonthAiCount(businessId))
                .aiLimit(planId != null ? planLimitService.getLimitValueOrDefault(planId, "AI_CREDITS", 0L) : 0L)
                .build();

        return ResponseEntity.ok(dto);
    }
}
