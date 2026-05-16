package com.example.smartBiz.controller;

import com.example.smartBiz.dto.ChatMessageDto;
import com.example.smartBiz.dto.ChatResponseDto;
import com.example.smartBiz.entity.Subscription;
import com.example.smartBiz.security.CustomUserPrincipal;
import com.example.smartBiz.service.ChatbotService;
import com.example.smartBiz.service.PlanLimitService;
import com.example.smartBiz.service.SubscriptionService;
import com.example.smartBiz.service.UsageCounterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/api/ai")
@CrossOrigin
@Tag(name = "AI", description = "AI Chatbot — business-aware conversational assistant")
public class AiChatController {

    private final ChatbotService chatbotService;
    private final PlanLimitService planLimitService;
    private final UsageCounterService usageCounterService;
    private final SubscriptionService subscriptionService;

    public AiChatController(ChatbotService chatbotService,
                            PlanLimitService planLimitService,
                            UsageCounterService usageCounterService,
                            SubscriptionService subscriptionService) {
        this.chatbotService = chatbotService;
        this.planLimitService = planLimitService;
        this.usageCounterService = usageCounterService;
        this.subscriptionService = subscriptionService;
    }

    private boolean canUseAi(Long businessId) {
        Subscription sub = subscriptionService.getBusinessSubscription(businessId);
        if (sub == null || sub.getPlan() == null) return false;
        Long limit = planLimitService.getLimitValueOrDefault(sub.getPlan().getId(), "AI_CREDITS", 0L);
        if (limit == -1) return true;
        Long currentUsage = usageCounterService.getThisMonthAiCount(businessId);
        return currentUsage < limit;
    }

    @Operation(
        summary = "AI Chatbot",
        description = "Business-aware chatbot. Detects intent (INVOICE, SALES, CUSTOMER, STOCK, ADVISOR, GENERAL) " +
                      "and responds with live business data. Consumes 1 AI credit per message. " +
                      "Only answers business-related questions."
    )
    @PostMapping("/chat")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<?> chat(@RequestBody ChatMessageDto dto, Authentication auth) {
        CustomUserPrincipal principal = (CustomUserPrincipal) auth.getPrincipal();
        Long businessId = principal.getBusinessId();

        if (!canUseAi(businessId)) {
            return ResponseEntity.status(403).body(
                ChatResponseDto.builder()
                    .reply("You've reached your AI credit limit for this month. Please upgrade your plan to continue.")
                    .intent("BLOCKED")
                    .build()
            );
        }

        if (dto.getMessage() == null || dto.getMessage().isBlank()) {
            return ResponseEntity.badRequest().body(
                ChatResponseDto.builder()
                    .reply("Please enter a message.")
                    .intent("NONE")
                    .build()
            );
        }

        ChatResponseDto response = chatbotService.chat(businessId, dto.getMessage().trim(), dto.getHistory());
        return ResponseEntity.ok(response);
    }
}
