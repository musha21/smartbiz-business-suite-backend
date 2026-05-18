package com.example.smartBiz.service;

import com.example.smartBiz.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CashRegisterService {

    CashRegisterSessionResponse openRegister(OpenRegisterRequest request);

    CashRegisterSessionResponse closeRegister(Long sessionId, CloseRegisterRequest request);

    CashRegisterSessionResponse getCurrentSession(Long businessId);

    CashRegisterSessionResponse getSessionById(Long sessionId);

    RegisterTransactionResponse addCashIn(Long sessionId, CashInOutRequest request);

    RegisterTransactionResponse addCashOut(Long sessionId, CashInOutRequest request);

    DailyReportResponse generateDailyReport(Long sessionId);

    Page<CashRegisterSessionResponse> getSessionHistory(Long businessId, Pageable pageable);

    List<RegisterTransactionResponse> getSessionTransactions(Long sessionId);
}
