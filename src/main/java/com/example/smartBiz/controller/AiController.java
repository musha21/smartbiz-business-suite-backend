package com.example.smartBiz.controller;

import com.example.smartBiz.dto.AiReportRequestDto;
import com.example.smartBiz.dto.AiReportResponseDto;
import com.example.smartBiz.dto.EmailDraftRequestDto;
import com.example.smartBiz.entity.Subscription;
import com.example.smartBiz.security.CustomUserPrincipal;
import com.example.smartBiz.service.AiService;
import com.example.smartBiz.service.PlanLimitService;
import com.example.smartBiz.service.SubscriptionService;
import com.example.smartBiz.service.UsageCounterService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/api/ai")
@CrossOrigin
public class AiController {

    private final AiService aiService;
    private final PlanLimitService planLimitService;
    private final UsageCounterService usageCounterService;
    private final SubscriptionService subscriptionService;

    public AiController(AiService aiService,
            PlanLimitService planLimitService,
            UsageCounterService usageCounterService,
            SubscriptionService subscriptionService) {
        this.aiService = aiService;
        this.planLimitService = planLimitService;
        this.usageCounterService = usageCounterService;
        this.subscriptionService = subscriptionService;
    }

    private boolean canUseAi(Long businessId) {
        Subscription sub = subscriptionService.getBusinessSubscription(businessId);
        if (sub == null || sub.getPlan() == null)
            return false;

        Long limit = planLimitService.getLimitValueOrDefault(sub.getPlan().getId(), "AI_CREDITS", 0L);
        if (limit == -1)
            return true; // Unlimited

        Long currentUsage = usageCounterService.getThisMonthAiCount(businessId);
        return currentUsage < limit;
    }

    @PostMapping("/report")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<AiReportResponseDto> getReport(
            @RequestBody AiReportRequestDto dto,
            Authentication auth) {
        CustomUserPrincipal principal = (CustomUserPrincipal) auth.getPrincipal();
        Long businessId = principal.getBusinessId();

        if (!canUseAi(businessId)) {
            // Returning a response with an error message in the report field if limit
            // reached
            return ResponseEntity.status(403)
                    .body(AiReportResponseDto.builder()
                            .aiReport("AI credit limit reached for this month.")
                            .build());
        }

        return ResponseEntity.ok(aiService.generateReport(
                businessId,
                dto.getFrom(),
                dto.getTo(),
                dto.getPrompt()));
    }
    // ✅ FIXED
    @PostMapping("/marketing/post")
    public ResponseEntity<?> generatePost(@RequestBody Map<String, Object> request) {
        CustomUserPrincipal principal = CustomUserPrincipal.getCurrent();
        if (principal == null || principal.getBusinessId() == null)
            return ResponseEntity.status(401).body("Unauthorized");
        Long businessId = principal.getBusinessId();

        if (!canUseAi(businessId)) {
            return ResponseEntity.status(403).body("AI credit limit reached for this month.");
        }

        // ✅ Safe cast - topic is a plain string
        String topic = request.getOrDefault("topic", "New Arrivals").toString();
        String post = aiService.generateMarketingPost(businessId, topic);
        return ResponseEntity.ok(Map.of("post", post));
    }

    @PostMapping("/email/draft")
    public ResponseEntity<?> generateEmailDraft(@RequestBody EmailDraftRequestDto req) {

        CustomUserPrincipal principal = CustomUserPrincipal.getCurrent();
        if (principal == null || principal.getBusinessId() == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        Long businessId = principal.getBusinessId();

        if (!canUseAi(businessId)) {
            return ResponseEntity.status(403).body("AI credit limit reached for this month.");
        }

        // Basic validation
        if (req.getCategory() == null || req.getTone() == null) {
            return ResponseEntity.badRequest().body("category and tone are required");
        }

        String email = aiService.generateEmailDraft(
                businessId,
                req.getCategory(),
                req.getTone(),
                req.getContext() == null ? "" : req.getContext()
        );

        return ResponseEntity.ok(java.util.Map.of("email", email));
    }
}
