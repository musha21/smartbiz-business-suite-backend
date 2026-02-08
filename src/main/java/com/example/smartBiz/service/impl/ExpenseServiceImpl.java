package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.ExpenseDto;
import com.example.smartBiz.entity.Expense;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.ExpenseRepo;
import com.example.smartBiz.security.RequestContext;
import com.example.smartBiz.service.ExpenseService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepo expenseRepository;
    private final RequestContext requestContext;

    public ExpenseServiceImpl(ExpenseRepo expenseRepository, RequestContext requestContext) {
        this.expenseRepository = expenseRepository;
        this.requestContext = requestContext;
    }

    // ✅ helper (reduce duplicate)
    private Long requireBusinessId() {
        Long businessId = requestContext.getBusinessId();
        if (businessId == null) {
            throw new RuntimeException("Business context missing (JWT required)");
        }
        return businessId;
    }

    // ✅ ownership check
    private Expense requireOwnedExpense(Long id, Long businessId) {
        Expense e = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id " + id));

        if (e.getBusinessId() == null || !e.getBusinessId().equals(businessId)) {
            throw new RuntimeException("Access denied: expense not in your business");
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

        return toDto(expenseRepository.save(expense));
    }

    @Override
    public ExpenseDto updateExpense(Long id, ExpenseDto dto) {
        Long businessId = requireBusinessId();
        Expense expense = requireOwnedExpense(id, businessId);

        if (dto.getExpenseDate() != null) expense.setExpenseDate(dto.getExpenseDate());
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
        return dto;
    }
}
