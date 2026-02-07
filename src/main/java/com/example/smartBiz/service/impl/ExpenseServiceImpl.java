package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.ExpenseDto;
import com.example.smartBiz.entity.Expense;
import com.example.smartBiz.repository.ExpenseRepo;
import com.example.smartBiz.service.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepo expenseRepository;

    @Autowired
    public ExpenseServiceImpl(ExpenseRepo expenseRepository) {
        this.expenseRepository = expenseRepository;
    }


    @Override
    public ExpenseDto createExpense(ExpenseDto dto) {
        Expense expense = new Expense();
        expense.setExpenseDate(dto.getExpenseDate() != null ? dto.getExpenseDate() : LocalDateTime.now());
        expense.setCategory(dto.getCategory());
        expense.setAmount(dto.getAmount());
        expense.setNote(dto.getNote());

        Expense saved = expenseRepository.save(expense);
        return toDto(saved);
    }

    @Override
    public ExpenseDto updateExpense(Long id, ExpenseDto dto) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        expense.setExpenseDate(dto.getExpenseDate() != null ? dto.getExpenseDate() : expense.getExpenseDate());
        expense.setCategory(dto.getCategory());
        expense.setAmount(dto.getAmount());
        expense.setNote(dto.getNote());

        Expense saved = expenseRepository.save(expense);
        return toDto(saved);
    }

    @Override
    public void deleteExpense(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        expenseRepository.delete(expense);
    }

    @Override
    public ExpenseDto getExpenseById(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        return toDto(expense);
    }

    @Override
    public List<ExpenseDto> getAllExpenses() {
        return expenseRepository.findAll()
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
