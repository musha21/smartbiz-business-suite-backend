package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.AssignPlanRequestDto;
import com.example.smartBiz.dto.CancelSubscriptionResponseDto;
import com.example.smartBiz.dto.MySubscriptionDto;
import com.example.smartBiz.entity.Business;
import com.example.smartBiz.entity.Plan;
import com.example.smartBiz.entity.Subscription;
import com.example.smartBiz.enums.BillingCycle;
import com.example.smartBiz.enums.SubscriptionStatus;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.BusinessRepo;
import com.example.smartBiz.repository.PlanRepo;
import com.example.smartBiz.repository.SubscriptionRepo;
import com.example.smartBiz.service.PlanLimitService;
import com.example.smartBiz.service.SubscriptionService;
import com.example.smartBiz.service.UsageCounterService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepo subscriptionRepo;
    private final BusinessRepo businessRepo;
    private final PlanRepo planRepo;
    private final PlanLimitService planLimitService;
    private final UsageCounterService usageCounterService;

    private static final DateTimeFormatter YM_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    public SubscriptionServiceImpl(
            SubscriptionRepo subscriptionRepo,
            BusinessRepo businessRepo,
            PlanRepo planRepo,
            PlanLimitService planLimitService,
            UsageCounterService usageCounterService) {
        this.subscriptionRepo = subscriptionRepo;
        this.businessRepo = businessRepo;
        this.planRepo = planRepo;
        this.planLimitService = planLimitService;
        this.usageCounterService = usageCounterService;
    }

    // ─── Admin: assign plan to business ───────────────────

    @Override
    @Transactional
    public Subscription assignPlan(AssignPlanRequestDto dto) {

        Business business = businessRepo.findById(dto.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("Business not found: " + dto.getBusinessId()));

        Plan plan = planRepo.findById(dto.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + dto.getPlanId()));

        // ✅ Block assignment of INACTIVE plans
        if (!com.example.smartBiz.enums.PlanStatus.ACTIVE.equals(plan.getStatus())) {
            throw new ResourceNotFoundException("Plan is " + plan.getStatus() + " and cannot be newly assigned.");
        }

        // Cancel any existing ACTIVE subscription for this business
        Optional<Subscription> existingOpt = subscriptionRepo
                .findByBusinessIdAndStatus(dto.getBusinessId(), SubscriptionStatus.ACTIVE);

        existingOpt.ifPresent(existing -> {
            existing.setStatus(SubscriptionStatus.CANCELED);
            subscriptionRepo.save(existing);
        });

        // Parse billing cycle
        BillingCycle cycle = BillingCycle.valueOf(dto.getBillingCycle().toUpperCase());

        // Calculate start / end
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endAt = null;

        // For free plans (price = 0), endAt = 7-day trial
        boolean isFree = plan.getMonthlyPrice() == 0.0 && plan.getYearlyPrice() == 0.0;

        if (isFree) {
            endAt = now.plusDays(7); // 7-day free trial
        } else {
            String unit = dto.getDurationUnit().toUpperCase();
            int count = dto.getDurationCount();

            if ("MONTHS".equals(unit)) {
                endAt = now.plusMonths(count);
            } else if ("YEARS".equals(unit)) {
                endAt = now.plusYears(count);
            } else {
                throw new ResourceNotFoundException("Invalid durationUnit: " + dto.getDurationUnit() + ". Use MONTHS or YEARS.");
            }
        }

        // Create new subscription
        Subscription sub = new Subscription();
        sub.setBusiness(business);
        sub.setPlan(plan);
        sub.setBillingCycle(cycle);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setStartAt(now);
        sub.setEndAt(endAt);

        // ✅ Sync business entity for easier analytics/filtering
        business.setPlanId(plan.getId());
        business.setPlan(plan.getName() != null ? plan.getName().trim() : null);
        business.setSubscriptionStart(now);
        business.setSubscriptionEnd(endAt);
        businessRepo.save(business);

        return subscriptionRepo.save(sub);
    }

    // ─── Owner: get my subscription ───────────────────────

    @Override
    public MySubscriptionDto getMySubscription(Long businessId) {

        // Auto-expire if needed
        refreshExpiryIfNeeded(businessId);

        Optional<Subscription> subOpt = subscriptionRepo
                .findByBusinessIdAndStatus(businessId, SubscriptionStatus.ACTIVE);

        if (subOpt.isEmpty()) {
            // No active subscription — return empty DTO
            MySubscriptionDto dto = new MySubscriptionDto();
            dto.setBusinessId(businessId);
            dto.setStatus("NONE");
            dto.setYearMonth(LocalDate.now().format(YM_FORMATTER));
            dto.setInvoicesUsed(usageCounterService.getThisMonthInvoiceCount(businessId));
            return dto;
        }

        Subscription sub = subOpt.get();
        Plan plan = sub.getPlan();
        Map<String, Long> limits = planLimitService.getLimitsMap(plan.getId());

        MySubscriptionDto dto = new MySubscriptionDto();
        dto.setBusinessId(businessId);
        dto.setPlanId(plan.getId());
        dto.setPlanCode(plan.getCode());
        dto.setPlanName(plan.getName());
        dto.setStatus(sub.getStatus().name());
        dto.setBillingCycle(sub.getBillingCycle().name());
        dto.setStartAt(sub.getStartAt());
        dto.setEndAt(sub.getEndAt());
        dto.setLimits(limits);
        dto.setYearMonth(LocalDate.now().format(YM_FORMATTER));
        dto.setInvoicesUsed(usageCounterService.getThisMonthInvoiceCount(businessId));
        dto.setCanceledAt(sub.getCanceledAt());
        dto.setCanceling(sub.getCanceledAt() != null
                && sub.getStatus() == SubscriptionStatus.ACTIVE);

        return dto;
    }

    // ─── Auto-expire check ───────────────────────────────

    @Override
    @Transactional
    public void refreshExpiryIfNeeded(Long businessId) {
        Optional<Subscription> subOpt = subscriptionRepo
                .findByBusinessIdAndStatus(businessId, SubscriptionStatus.ACTIVE);

        if (subOpt.isEmpty())
            return;

        Subscription sub = subOpt.get();

        // If endAt is set and has passed → transition status
        if (sub.getEndAt() != null && sub.getEndAt().isBefore(LocalDateTime.now())) {
            // If user had requested cancel → CANCELED, otherwise → EXPIRED
            if (sub.getCanceledAt() != null) {
                sub.setStatus(SubscriptionStatus.CANCELED);
            } else {
                sub.setStatus(SubscriptionStatus.EXPIRED);
            }
            subscriptionRepo.save(sub);
            syncBusinessOnCancel(sub.getBusiness());
        }
    }

    @Override
    public com.example.smartBiz.dto.MyPlanDto getOwnerPlan(Long businessId) {
        // Auto-expire check before fetching
        refreshExpiryIfNeeded(businessId);

        Optional<Subscription> subOpt = subscriptionRepo
                .findFirstByBusinessIdOrderByCreatedAtDesc(businessId);

        if (subOpt.isEmpty()) {
            return new com.example.smartBiz.dto.MyPlanDto(null, null, null, "NONE");
        }

        Subscription sub = subOpt.get();
        String status;
        switch (sub.getStatus()) {
            case EXPIRED -> status = "EXPIRED";
            case CANCELED -> status = "CANCELED";
            case PENDING_PAYMENT -> status = "PENDING_PAYMENT";
            default -> {
                // ACTIVE — but check if cancel was requested (grace period)
                if (sub.getCanceledAt() != null) {
                    status = "CANCELING";
                } else {
                    status = "ACTIVE";
                }
            }
        }

        return new com.example.smartBiz.dto.MyPlanDto(
                sub.getPlan().getId(),
                sub.getPlan().getName(),
                sub.getEndAt(),
                status);
    }

    @Override
    public Subscription getBusinessSubscription(Long businessId) {
        return subscriptionRepo.findByBusinessIdAndStatus(businessId, SubscriptionStatus.ACTIVE)
                .orElse(null);
    }

    // ─── Cancel subscription ──────────────────────────

    @Override
    @Transactional
    public CancelSubscriptionResponseDto cancelSubscription(Long businessId) {
        Subscription sub = subscriptionRepo
                .findByBusinessIdAndStatus(businessId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active subscription found for business: " + businessId));

        // Free plans cannot be canceled
        Plan plan = sub.getPlan();
        if (plan.getMonthlyPrice() == 0.0 && plan.getYearlyPrice() == 0.0) {
            throw new IllegalStateException("Free plan subscriptions cannot be canceled.");
        }

        // Already cancel-requested?
        if (sub.getCanceledAt() != null) {
            return CancelSubscriptionResponseDto.builder()
                    .status("CANCELING")
                    .message("Cancellation already requested. Access continues until " + sub.getEndAt() + ".")
                    .canceledAt(sub.getCanceledAt())
                    .accessUntil(sub.getEndAt())
                    .build();
        }

        sub.setCanceledAt(LocalDateTime.now());

        if (sub.getEndAt() == null) {
            // No end date → cancel immediately
            sub.setStatus(SubscriptionStatus.CANCELED);
            subscriptionRepo.save(sub);
            syncBusinessOnCancel(sub.getBusiness());

            return CancelSubscriptionResponseDto.builder()
                    .status("CANCELED")
                    .message("Subscription canceled immediately.")
                    .canceledAt(sub.getCanceledAt())
                    .accessUntil(null)
                    .build();
        }

        // Has end date → keep active until endAt (grace period)
        subscriptionRepo.save(sub);

        return CancelSubscriptionResponseDto.builder()
                .status("CANCELING")
                .message("Subscription will end on " + sub.getEndAt()
                        + ". You retain full access until then.")
                .canceledAt(sub.getCanceledAt())
                .accessUntil(sub.getEndAt())
                .build();
    }

    @Override
    @Transactional
    public CancelSubscriptionResponseDto adminCancelSubscription(Long businessId, boolean immediate) {
        Subscription sub = subscriptionRepo
                .findByBusinessIdAndStatus(businessId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active subscription for business: " + businessId));

        sub.setCanceledAt(LocalDateTime.now());

        if (immediate) {
            sub.setStatus(SubscriptionStatus.CANCELED);
            subscriptionRepo.save(sub);
            syncBusinessOnCancel(sub.getBusiness());
        } else {
            // Grace period — stays ACTIVE until endAt
            subscriptionRepo.save(sub);
        }

        String msg = immediate
                ? "Subscription canceled immediately by admin."
                : "Subscription marked for cancellation at period end.";

        return CancelSubscriptionResponseDto.builder()
                .status(immediate ? "CANCELED" : "CANCELING")
                .message(msg)
                .canceledAt(sub.getCanceledAt())
                .accessUntil(immediate ? null : sub.getEndAt())
                .build();
    }

    /** Clear Business plan fields when subscription is fully canceled */
    private void syncBusinessOnCancel(Business business) {
        business.setPlanId(null);
        business.setPlan(null);
        business.setSubscriptionStart(null);
        business.setSubscriptionEnd(null);
        businessRepo.save(business);
    }
}
