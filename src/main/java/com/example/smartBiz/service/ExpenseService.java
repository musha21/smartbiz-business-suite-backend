package com.example.smartBiz.service;

import com.example.smartBiz.dto.ExpenseDto;

import java.util.List;
import java.util.Map;

public interface ExpenseService {

    ExpenseDto createExpense(ExpenseDto dto);

    ExpenseDto updateExpense(Long id, ExpenseDto dto);

    void deleteExpense(Long id);

    ExpenseDto getExpenseById(Long id);

    List<ExpenseDto> getAllExpenses();

    Map<String, Object> getExpenseStats();

    List<ExpenseDto> getExpensesByCashRegisterSessionId(Long sessionId);
}
