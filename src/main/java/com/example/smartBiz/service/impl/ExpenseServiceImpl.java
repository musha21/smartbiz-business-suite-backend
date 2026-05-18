package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.ExpenseDto;
import com.example.smartBiz.entity.Expense;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.ExpenseRepo;
import com.example.smartBiz.security.CustomUserPrincipal;
import com.example.smartBiz.service.ExpenseService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepo expenseRepository;

    public ExpenseServiceImpl(ExpenseRepo expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    // ✅ helper (reduce duplicate)
    private Long requireBusinessId() {
        CustomUserPrincipal principal = CustomUserPrincipal.getCurrent();
        if (principal == null || principal.getBusinessId() == null) {
            throw new ResourceNotFoundException("Business context missing (JWT required)");
        }
        return principal.getBusinessId();
    }

    // ✅ ownership check
    private Expense requireOwnedExpense(Long id, Long businessId) {
        Expense e = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id " + id));

        if (e.getBusinessId() == null || !e.getBusinessId().equals(businessId)) {
            throw new ResourceNotFoundException("Access denied: expense not in your business");
        }
        return e;
    }

    @Override
    public ExpenseDto createExpense(ExpenseDto dto) {
        Long businessId = requireBusinessId();

        Expense expense = new Expense();
        expense.setExpenseDate(dto.getExpenseDate() != null ? dto.getExpenseDate() : LocalDateTime.now());
        expense.setCategory(dto.getCategory());
        expense.setAmount(dto.getAmount());
        expense.setNote(dto.getNote());
        expense.setBusinessId(businessId);
        
        // ✅ Link to cash register session if provided
        if (dto.getCashRegisterSessionId() != null) {
            expense.setCashRegisterSessionId(dto.getCashRegisterSessionId());
        }

        return toDto(expenseRepository.save(expense));
    }

    @Override
    public ExpenseDto updateExpense(Long id, ExpenseDto dto) {
        Long businessId = requireBusinessId();
        Expense expense = requireOwnedExpense(id, businessId);

        if (dto.getExpenseDate() != null)
            expense.setExpenseDate(dto.getExpenseDate());
        expense.setCategory(dto.getCategory());
        expense.setAmount(dto.getAmount());
        expense.setNote(dto.getNote());

        return toDto(expenseRepository.save(expense));
    }

    @Override
    public void deleteExpense(Long id) {
        Long businessId = requireBusinessId();
        Expense expense = requireOwnedExpense(id, businessId);
        expenseRepository.delete(expense);
    }

    @Override
    public ExpenseDto getExpenseById(Long id) {
        Long businessId = requireBusinessId();
        Expense expense = requireOwnedExpense(id, businessId);
        return toDto(expense);
    }

    @Override
    public List<ExpenseDto> getAllExpenses() {
        Long businessId = requireBusinessId();

        return expenseRepository.findByBusinessId(businessId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private ExpenseDto toDto(Expense e) {
        ExpenseDto dto = new ExpenseDto();
        dto.setId(e.getId());
        dto.setExpenseDate(e.getExpenseDate());
        dto.setCategory(e.getCategory());
        dto.setAmount(e.getAmount());
        dto.setNote(e.getNote());
        dto.setCashRegisterSessionId(e.getCashRegisterSessionId());
        return dto;
    }

    @Override
    public List<ExpenseDto> getExpensesByCashRegisterSessionId(Long sessionId) {
        requireBusinessId(); // verify auth
        return expenseRepository.findByCashRegisterSessionId(sessionId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public Map<String, Object> getExpenseStats() {
        Long businessId = requireBusinessId();
        List<Expense> expenses = expenseRepository.findByBusinessId(businessId);

        double totalAmount = expenses.stream()
                .mapToDouble(e -> e.getAmount().doubleValue())
                .sum();

        Map<String, Long> byCategory = expenses.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        Expense::getCategory,
                        java.util.stream.Collectors.counting()
                ));

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", expenses.size());
        stats.put("totalAmount", totalAmount);
        stats.put("byCategory", byCategory);

        return stats;
    }
}
