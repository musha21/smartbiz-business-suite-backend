package com.example.smartBiz.controller;

import com.example.smartBiz.dto.*;
import com.example.smartBiz.response.ApiResponse;
import com.example.smartBiz.service.CashRegisterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/api/cash-register")
@RequiredArgsConstructor
public class CashRegisterController {

    private final CashRegisterService cashRegisterService;

    @PostMapping("/open")
    public ResponseEntity<ApiResponse<CashRegisterSessionResponse>> openRegister(
            @Valid @RequestBody OpenRegisterRequest request) {
        CashRegisterSessionResponse session = cashRegisterService.openRegister(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Register opened successfully", session));
    }

    @PostMapping("/{sessionId}/close")
    public ResponseEntity<ApiResponse<CashRegisterSessionResponse>> closeRegister(
            @PathVariable Long sessionId,
            @Valid @RequestBody CloseRegisterRequest request) {
        CashRegisterSessionResponse session = cashRegisterService.closeRegister(sessionId, request);
        return ResponseEntity.ok(ApiResponse.success("Register closed successfully", session));
    }

    @GetMapping("/current/{businessId}")
    public ResponseEntity<ApiResponse<CashRegisterSessionResponse>> getCurrentSession(
            @PathVariable Long businessId) {
        CashRegisterSessionResponse session = cashRegisterService.getCurrentSession(businessId);
        if (session == null) {
            return ResponseEntity.ok(ApiResponse.success("No open register session found", null));
        }
        return ResponseEntity.ok(ApiResponse.success("Current session retrieved", session));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<CashRegisterSessionResponse>> getSessionById(
            @PathVariable Long sessionId) {
        CashRegisterSessionResponse session = cashRegisterService.getSessionById(sessionId);
        return ResponseEntity.ok(ApiResponse.success("Session retrieved", session));
    }

    @PostMapping("/{sessionId}/cash-in")
    public ResponseEntity<ApiResponse<RegisterTransactionResponse>> addCashIn(
            @PathVariable Long sessionId,
            @Valid @RequestBody CashInOutRequest request) {
        RegisterTransactionResponse transaction = cashRegisterService.addCashIn(sessionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Cash in recorded successfully", transaction));
    }

    @PostMapping("/{sessionId}/cash-out")
    public ResponseEntity<ApiResponse<RegisterTransactionResponse>> addCashOut(
            @PathVariable Long sessionId,
            @Valid @RequestBody CashInOutRequest request) {
        RegisterTransactionResponse transaction = cashRegisterService.addCashOut(sessionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Cash out recorded successfully", transaction));
    }

    @GetMapping("/{sessionId}/report")
    public ResponseEntity<ApiResponse<DailyReportResponse>> generateDailyReport(
            @PathVariable Long sessionId) {
        DailyReportResponse report = cashRegisterService.generateDailyReport(sessionId);
        return ResponseEntity.ok(ApiResponse.success("Daily report generated", report));
    }

    @GetMapping("/{sessionId}/transactions")
    public ResponseEntity<ApiResponse<List<RegisterTransactionResponse>>> getSessionTransactions(
            @PathVariable Long sessionId) {
        List<RegisterTransactionResponse> transactions = cashRegisterService.getSessionTransactions(sessionId);
        return ResponseEntity.ok(ApiResponse.success("Transactions retrieved", transactions));
    }

    @GetMapping("/history/{businessId}")
    public ResponseEntity<ApiResponse<Page<CashRegisterSessionResponse>>> getSessionHistory(
            @PathVariable Long businessId,
            Pageable pageable) {
        Page<CashRegisterSessionResponse> history = cashRegisterService.getSessionHistory(businessId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Session history retrieved", history));
    }
}
