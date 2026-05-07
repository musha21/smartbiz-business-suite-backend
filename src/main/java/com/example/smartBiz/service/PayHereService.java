package com.example.smartBiz.service;

import com.example.smartBiz.dto.CheckoutRequestDto;
import com.example.smartBiz.dto.CheckoutResponseDto;
import com.example.smartBiz.dto.PaymentHistoryDto;

import java.util.List;
import java.util.Map;

public interface PayHereService {

    /**
     * Initiates a PayHere checkout session for the given business and plan.
     *
     * @param businessId the authenticated user's business
     * @param userId     the authenticated user's ID (used to prefill checkout form)
     * @param request    plan and billing-cycle selection
     */
    CheckoutResponseDto initiateCheckout(Long businessId, Long userId, CheckoutRequestDto request);

    /**
     * Processes the PayHere server-to-server notification callback.
     * Verifies the MD5 hash and activates the subscription if payment is successful.
     *
     * @param params all form parameters from PayHere notify POST
     */
    void handleNotify(Map<String, String> params);

    /**
     * Returns the current status of a payment order.
     */
    String getOrderStatus(String orderId);

    /**
     * Returns the payment history for a business, newest first.
     */
    List<PaymentHistoryDto> getPaymentHistory(Long businessId);
}
