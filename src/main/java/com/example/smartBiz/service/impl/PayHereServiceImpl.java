package com.example.smartBiz.service.impl;

import com.example.smartBiz.config.PayHereConfig;
import com.example.smartBiz.dto.CheckoutRequestDto;
import com.example.smartBiz.dto.CheckoutResponseDto;
import com.example.smartBiz.dto.PaymentHistoryDto;
import com.example.smartBiz.entity.*;
import com.example.smartBiz.enums.BillingCycle;
import com.example.smartBiz.enums.SubscriptionStatus;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.*;
import com.example.smartBiz.service.PayHereService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PayHereServiceImpl implements PayHereService {

    private static final Logger log = LoggerFactory.getLogger(PayHereServiceImpl.class);

    private final PayHereConfig payHereConfig;
    private final PaymentOrderRepo paymentOrderRepo;
    private final PlanRepo planRepo;
    private final BusinessRepo businessRepo;
    private final SubscriptionRepo subscriptionRepo;
    private final UserRepo userRepo;

    public PayHereServiceImpl(PayHereConfig payHereConfig,
                              PaymentOrderRepo paymentOrderRepo,
                              PlanRepo planRepo,
                              BusinessRepo businessRepo,
                              SubscriptionRepo subscriptionRepo,
                              UserRepo userRepo) {
        this.payHereConfig = payHereConfig;
        this.paymentOrderRepo = paymentOrderRepo;
        this.planRepo = planRepo;
        this.businessRepo = businessRepo;
        this.subscriptionRepo = subscriptionRepo;
        this.userRepo = userRepo;
    }

    // ─── Checkout initiation ──────────────────────────────────

    @Override
    @Transactional
    public CheckoutResponseDto initiateCheckout(Long businessId, Long userId, CheckoutRequestDto request) {

        Business business = businessRepo.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found: " + businessId));

        Plan plan = planRepo.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + request.getPlanId()));

        if (!com.example.smartBiz.enums.PlanStatus.ACTIVE.equals(plan.getStatus())) {
            throw new ResourceNotFoundException("Plan is " + plan.getStatus() + " and cannot be purchased.");
        }

        BillingCycle cycle = BillingCycle.valueOf(request.getBillingCycle().toUpperCase());

        double amount = cycle == BillingCycle.MONTHLY
                ? plan.getMonthlyPrice()
                : plan.getYearlyPrice();

        if (amount <= 0) {
            throw new ResourceNotFoundException("Cannot checkout a free plan via PayHere. Use free plan assignment instead.");
        }

        String orderId = "SB-" + businessId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String amountFormatted = String.format(Locale.US, "%.2f", amount);

        // Create payment order record
        PaymentOrder order = PaymentOrder.builder()
                .orderId(orderId)
                .business(business)
                .plan(plan)
                .billingCycle(cycle.name())
                .amount(amount)
                .currency(payHereConfig.getCurrency())
                .status("PENDING")
                .build();
        paymentOrderRepo.save(order);

        // No subscription created here — subscription is only created
        // when PayHere confirms payment success (in activateSubscription).

        // Generate PayHere hash
        String hash = generateCheckoutHash(orderId, amountFormatted, payHereConfig.getCurrency());

        // Lookup owner for prefilling checkout form
        String firstName = business.getName();
        String email = "";
        AppUser owner = userRepo.findById(userId).orElse(null);
        if (owner != null) {
            firstName = owner.getName();
            email = owner.getEmail();
        }

        return CheckoutResponseDto.builder()
                .merchantId(payHereConfig.getMerchantId())
                .orderId(orderId)
                .itemsDescription(plan.getName() + " - " + cycle.name() + " Subscription")
                .currency(payHereConfig.getCurrency())
                .amountFormatted(amountFormatted)
                .firstName(firstName)
                .lastName("")
                .email(email)
                .phone("")
                .address("")
                .city("")
                .country("Sri Lanka")
                .notifyUrl(payHereConfig.getNotifyUrl())
                .returnUrl(payHereConfig.getReturnUrl() + "?order_id=" + orderId)
                .cancelUrl(payHereConfig.getCancelUrl())
                .hash(hash)
                .checkoutUrl(payHereConfig.getCheckoutUrl())
                .build();
    }

    // ─── PayHere notify callback ──────────────────────────────

    @Override
    @Transactional
    public void handleNotify(Map<String, String> params) {
        String merchantId = params.get("merchant_id");
        String orderId = params.get("order_id");
        String payhereAmount = params.get("payhere_amount");
        String payhereCurrency = params.get("payhere_currency");
        String statusCode = params.get("status_code");
        String md5sig = params.get("md5sig");
        String paymentId = params.get("payment_id");

        log.info("PayHere notify received — orderId={}, statusCode={}, paymentId={}", orderId, statusCode, paymentId);

        // 1. Verify merchant ID
        if (!payHereConfig.getMerchantId().equals(merchantId)) {
            log.warn("PayHere notify rejected — merchant_id mismatch: {}", merchantId);
            return;
        }

        // 2. Find the payment order
        PaymentOrder order = paymentOrderRepo.findByOrderId(orderId).orElse(null);
        if (order == null) {
            log.warn("PayHere notify — unknown order_id: {}", orderId);
            return;
        }

        // 3. Idempotency check
        if ("COMPLETED".equals(order.getStatus())) {
            log.info("PayHere notify — order {} already completed, skipping.", orderId);
            return;
        }

        // 4. Verify MD5 signature
        String localMd5 = generateNotifyHash(merchantId, orderId, payhereAmount, payhereCurrency, statusCode);
        if (!localMd5.equals(md5sig)) {
            log.warn("PayHere notify rejected — MD5 mismatch for order {}", orderId);
            order.setStatus("HASH_FAILED");
            paymentOrderRepo.save(order);
            return;
        }

        // 5. Update payment order
        int code = Integer.parseInt(statusCode);
        order.setPayhereStatusCode(code);
        order.setPayherePaymentId(paymentId);
        order.setPayhereMd5sig(md5sig);

        if (code == 2) {
            // ── Payment successful ──
            order.setStatus("COMPLETED");
            order.setPaidAt(LocalDateTime.now());
            paymentOrderRepo.save(order);

            activateSubscription(order);
            log.info("PayHere payment successful — order {} activated.", orderId);

        } else if (code == 0) {
            order.setStatus("PENDING");
            paymentOrderRepo.save(order);
            log.info("PayHere notify — order {} still pending.", orderId);

        } else if (code == -1) {
            order.setStatus("CANCELED");
            paymentOrderRepo.save(order);
            cancelPendingSubscription(orderId);
            log.info("PayHere notify — order {} canceled.", orderId);

        } else if (code == -2) {
            order.setStatus("FAILED");
            paymentOrderRepo.save(order);
            cancelPendingSubscription(orderId);
            log.info("PayHere notify — order {} failed (chargedback/disputed).", orderId);

        } else {
            order.setStatus("UNKNOWN_" + code);
            paymentOrderRepo.save(order);
            log.warn("PayHere notify — unknown status_code {} for order {}", code, orderId);
        }
    }

    // ─── Order status ─────────────────────────────────────────

    @Override
    public String getOrderStatus(String orderId) {
        PaymentOrder order = paymentOrderRepo.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found: " + orderId));
        return order.getStatus();
    }

    // ─── Payment history ──────────────────────────────────────

    @Override
    public List<PaymentHistoryDto> getPaymentHistory(Long businessId) {
        List<PaymentOrder> orders = paymentOrderRepo.findByBusinessIdOrderByCreatedAtDesc(businessId);
        return orders.stream()
                .map(order -> PaymentHistoryDto.builder()
                        .orderId(order.getOrderId())
                        .planName(order.getPlan() != null ? order.getPlan().getName() : "Unknown")
                        .amount(order.getAmount())
                        .currency(order.getCurrency())
                        .status(order.getStatus())
                        .paidAt(order.getPaidAt())
                        .billingCycle(order.getBillingCycle())
                        .build())
                .toList();
    }

    // ─── Private helpers ──────────────────────────────────────

    private void activateSubscription(PaymentOrder order) {
        Long businessId = order.getBusiness().getId();

        // Cancel any existing ACTIVE subscription
        Optional<Subscription> existingOpt = subscriptionRepo
                .findByBusinessIdAndStatus(businessId, SubscriptionStatus.ACTIVE);
        existingOpt.ifPresent(existing -> {
            existing.setStatus(SubscriptionStatus.CANCELED);
            existing.setCanceledAt(LocalDateTime.now());
            subscriptionRepo.save(existing);
        });

        // Create new ACTIVE subscription directly from payment order
        Subscription sub = new Subscription();
        sub.setBusiness(order.getBusiness());
        sub.setPlan(order.getPlan());
        sub.setBillingCycle(BillingCycle.valueOf(order.getBillingCycle()));
        sub.setPayhereOrderId(order.getOrderId());
        sub.setPayherePaymentId(order.getPayherePaymentId());
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setStartAt(LocalDateTime.now());

        // Calculate end date
        BillingCycle cycle = BillingCycle.valueOf(order.getBillingCycle());
        LocalDateTime endAt = cycle == BillingCycle.MONTHLY
                ? LocalDateTime.now().plusMonths(1)
                : LocalDateTime.now().plusYears(1);
        sub.setEndAt(endAt);

        subscriptionRepo.save(sub);

        // Sync business entity
        Business business = order.getBusiness();
        Plan plan = order.getPlan();
        business.setPlanId(plan.getId());
        business.setPlan(plan.getName());
        business.setSubscriptionStart(sub.getStartAt());
        business.setSubscriptionEnd(endAt);
        businessRepo.save(business);
    }

    /** No-op: pending subscriptions are no longer created during checkout */
    private void cancelPendingSubscription(String orderId) {
        // PaymentOrder status already tracks cancellation/failure.
        // No PENDING_PAYMENT subscription to clean up.
        log.info("Payment canceled/failed for order {} — no pending subscription to clean up.", orderId);
    }

    /**
     * PayHere checkout hash:
     * MD5(merchant_id + order_id + amount + currency + MD5(merchant_secret).toUpperCase()).toUpperCase()
     */
    private String generateCheckoutHash(String orderId, String amountFormatted, String currency) {
        String merchantSecret = payHereConfig.getMerchantSecret();
        String hashedSecret = md5(merchantSecret).toUpperCase();
        String raw = payHereConfig.getMerchantId() + orderId + amountFormatted + currency + hashedSecret;
        return md5(raw).toUpperCase();
    }

    /**
     * PayHere notify verification hash:
     * MD5(merchant_id + order_id + payhere_amount + payhere_currency + status_code +
     *     MD5(merchant_secret).toUpperCase()).toUpperCase()
     */
    private String generateNotifyHash(String merchantId, String orderId,
                                       String payhereAmount, String payhereCurrency,
                                       String statusCode) {
        String hashedSecret = md5(payHereConfig.getMerchantSecret()).toUpperCase();
        String raw = merchantId + orderId + payhereAmount + payhereCurrency + statusCode + hashedSecret;
        return md5(raw).toUpperCase();
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new ResourceNotFoundException("MD5 algorithm not available", e);
        }
    }
}
