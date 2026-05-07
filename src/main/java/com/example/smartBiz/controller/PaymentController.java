package com.example.smartBiz.controller;

import com.example.smartBiz.dto.CheckoutRequestDto;
import com.example.smartBiz.dto.CheckoutResponseDto;
import com.example.smartBiz.dto.PaymentHistoryDto;
import com.example.smartBiz.security.CustomUserPrincipal;
import com.example.smartBiz.service.PayHereService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/api/payments")
@CrossOrigin
@Tag(name = "Payments", description = "PayHere payment gateway integration for subscription billing")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PayHereService payHereService;

    public PaymentController(PayHereService payHereService) {
        this.payHereService = payHereService;
    }

    @Operation(summary = "Initiate checkout",
            description = "Creates a PayHere payment order and returns the form data needed to redirect to PayHere checkout.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Checkout session created"),
            @ApiResponse(responseCode = "400", description = "Invalid plan or billing cycle"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponseDto> initiateCheckout(@Valid @RequestBody CheckoutRequestDto request) {
        CustomUserPrincipal principal = CustomUserPrincipal.getCurrent();
        if (principal == null || principal.getBusinessId() == null) {
            return ResponseEntity.status(401).build();
        }
        CheckoutResponseDto response = payHereService.initiateCheckout(
                principal.getBusinessId(), principal.getUserId(), request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "PayHere notify callback",
            description = "Server-to-server callback from PayHere after payment processing. " +
                    "This endpoint is public (no JWT required). PayHere sends payment status via POST form data.",
            security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification processed"),
            @ApiResponse(responseCode = "400", description = "Invalid notification data")
    })
    @PostMapping("/notify")
    public ResponseEntity<String> handleNotify(@RequestParam Map<String, String> params) {
        log.info("PayHere notify callback received: {}", params.get("order_id"));
        try {
            payHereService.handleNotify(params);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("PayHere notify error: {}", e.getMessage(), e);
            return ResponseEntity.ok("OK");
        }
    }

    @Operation(summary = "Get payment order status",
            description = "Returns the current status of a payment order (PENDING, COMPLETED, CANCELED, FAILED).")
    @ApiResponse(responseCode = "200", description = "Order status returned")
    @GetMapping("/status/{orderId}")
    public ResponseEntity<Map<String, String>> getOrderStatus(
            @Parameter(description = "Order ID returned from checkout") @PathVariable(name = "orderId") String orderId) {
        String status = payHereService.getOrderStatus(orderId);
        return ResponseEntity.ok(Map.of("orderId", orderId, "status", status));
    }

    @Operation(summary = "Get payment history",
            description = "Returns the payment history for the authenticated business, newest first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment history returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/history")
    public ResponseEntity<List<PaymentHistoryDto>> getPaymentHistory() {
        CustomUserPrincipal principal = CustomUserPrincipal.getCurrent();
        if (principal == null || principal.getBusinessId() == null) {
            return ResponseEntity.status(401).build();
        }
        List<PaymentHistoryDto> history = payHereService.getPaymentHistory(principal.getBusinessId());
        return ResponseEntity.ok(history);
    }
}

