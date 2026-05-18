package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.*;
import com.example.smartBiz.entity.CashRegisterSession;
import com.example.smartBiz.entity.RegisterTransaction;
import com.example.smartBiz.exception.NotFoundException;
import com.example.smartBiz.exception.ValidationException;
import com.example.smartBiz.repository.CashRegisterSessionRepo;
import com.example.smartBiz.repository.RegisterTransactionRepo;
import com.example.smartBiz.service.CashRegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CashRegisterServiceImpl implements CashRegisterService {

    private final CashRegisterSessionRepo sessionRepo;
    private final RegisterTransactionRepo transactionRepo;

    @Override
    @Transactional
    public CashRegisterSessionResponse openRegister(OpenRegisterRequest request) {
        // Check if there's already an open session for this business
        if (sessionRepo.existsByBusinessIdAndStatus(request.getBusinessId(), CashRegisterSession.SessionStatus.OPEN)) {
            throw new ValidationException("There is already an open register session for this business. Please close it first.");
        }

        CashRegisterSession session = CashRegisterSession.builder()
                .businessId(request.getBusinessId())
                .cashierId(request.getCashierId())
                .openingBalance(request.getOpeningBalance())
                .status(CashRegisterSession.SessionStatus.OPEN)
                .notes(request.getNotes())
                .cashSales(BigDecimal.ZERO)
                .cardSales(BigDecimal.ZERO)
                .otherSales(BigDecimal.ZERO)
                .totalExpenses(BigDecimal.ZERO)
                .build();

        session.calculateExpectedCash();
        CashRegisterSession saved = sessionRepo.save(session);
        return CashRegisterSessionResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public CashRegisterSessionResponse closeRegister(Long sessionId, CloseRegisterRequest request) {
        CashRegisterSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Register session not found with ID: " + sessionId));

        if (session.getStatus() == CashRegisterSession.SessionStatus.CLOSED) {
            throw new ValidationException("This register session is already closed");
        }

        session.setActualCash(request.getActualCash());
        session.calculateVariance();
        session.setStatus(CashRegisterSession.SessionStatus.CLOSED);
        session.setCloseTime(LocalDateTime.now());
        if (request.getNotes() != null) {
            session.setNotes(session.getNotes() + " | Close notes: " + request.getNotes());
        }

        CashRegisterSession saved = sessionRepo.save(session);
        return CashRegisterSessionResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CashRegisterSessionResponse getCurrentSession(Long businessId) {
        return sessionRepo.findByBusinessIdAndStatus(businessId, CashRegisterSession.SessionStatus.OPEN)
                .map(CashRegisterSessionResponse::fromEntity)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public CashRegisterSessionResponse getSessionById(Long sessionId) {
        CashRegisterSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Register session not found with ID: " + sessionId));
        return CashRegisterSessionResponse.fromEntity(session);
    }

    @Override
    @Transactional
    public RegisterTransactionResponse addCashIn(Long sessionId, CashInOutRequest request) {
        CashRegisterSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Register session not found with ID: " + sessionId));

        if (session.getStatus() == CashRegisterSession.SessionStatus.CLOSED) {
            throw new ValidationException("Cannot add cash to a closed register session");
        }

        RegisterTransaction transaction = RegisterTransaction.builder()
                .sessionId(sessionId)
                .businessId(session.getBusinessId())
                .type(RegisterTransaction.TransactionType.CASH_IN)
                .amount(request.getAmount())
                .paymentMethod(RegisterTransaction.PaymentMethod.CASH)
                .description(request.getDescription())
                .referenceNumber(request.getReferenceNumber())
                .build();

        RegisterTransaction saved = transactionRepo.save(transaction);
        return RegisterTransactionResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public RegisterTransactionResponse addCashOut(Long sessionId, CashInOutRequest request) {
        CashRegisterSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Register session not found with ID: " + sessionId));

        if (session.getStatus() == CashRegisterSession.SessionStatus.CLOSED) {
            throw new ValidationException("Cannot remove cash from a closed register session");
        }

        RegisterTransaction transaction = RegisterTransaction.builder()
                .sessionId(sessionId)
                .businessId(session.getBusinessId())
                .type(RegisterTransaction.TransactionType.CASH_OUT)
                .amount(request.getAmount())
                .paymentMethod(RegisterTransaction.PaymentMethod.CASH)
                .description(request.getDescription())
                .referenceNumber(request.getReferenceNumber())
                .build();

        RegisterTransaction saved = transactionRepo.save(transaction);
        return RegisterTransactionResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DailyReportResponse generateDailyReport(Long sessionId) {
        CashRegisterSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Register session not found with ID: " + sessionId));

        List<RegisterTransaction> transactions = transactionRepo.findBySessionIdOrderByTimestampDesc(sessionId);
        List<RegisterTransaction> expenses = transactions.stream()
                .filter(t -> t.getType() == RegisterTransaction.TransactionType.EXPENSE)
                .collect(Collectors.toList());
        List<RegisterTransaction> cashTransactions = transactions.stream()
                .filter(t -> t.getType() == RegisterTransaction.TransactionType.SALE && t.getPaymentMethod() == RegisterTransaction.PaymentMethod.CASH)
                .collect(Collectors.toList());

        DailyReportResponse report = new DailyReportResponse();
        report.setSessionId(session.getId());
        report.setCashierId(session.getCashierId());
        report.setOpenTime(session.getOpenTime());
        report.setCloseTime(session.getCloseTime());
        report.setStatus(session.getStatus().name());
        report.setOpeningBalance(session.getOpeningBalance());
        report.setCashSales(session.getCashSales());
        report.setCardSales(session.getCardSales());
        report.setOtherSales(session.getOtherSales());
        report.setTotalExpenses(session.getTotalExpenses());
        report.setExpectedCash(session.getExpectedCash());
        report.setActualCash(session.getActualCash());
        report.setVariance(session.getVariance());
        report.setCashTransactionCount(cashTransactions.size());
        report.setExpenseCount(expenses.size());
        report.setExpenses(expenses.stream().map(RegisterTransactionResponse::fromEntity).collect(Collectors.toList()));
        report.setCashTransactions(cashTransactions.stream().map(RegisterTransactionResponse::fromEntity).collect(Collectors.toList()));
        report.calculateTotals();

        return report;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CashRegisterSessionResponse> getSessionHistory(Long businessId, Pageable pageable) {
        return sessionRepo.findByBusinessIdOrderByOpenTimeDesc(businessId, pageable)
                .map(CashRegisterSessionResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegisterTransactionResponse> getSessionTransactions(Long sessionId) {
        return transactionRepo.findBySessionIdOrderByTimestampDesc(sessionId).stream()
                .map(RegisterTransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
