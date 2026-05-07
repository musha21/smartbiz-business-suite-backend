package com.example.smartBiz.controller;

import com.example.smartBiz.dto.ExpenseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.smartBiz.service.ExpenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/api/expenses")
@CrossOrigin
@Tag(name = "Expenses", description = "CRUD for business expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @Operation(summary = "Create an expense")
    @ApiResponse(responseCode = "200", description = "Expense created")
    @PostMapping
    public ResponseEntity<ExpenseDto> createExpense(@RequestBody ExpenseDto dto) {
        return ResponseEntity.ok(expenseService.createExpense(dto));
    }

    @Operation(summary = "Update an expense")
    @ApiResponse(responseCode = "200", description = "Expense updated")
    @PutMapping("/{id}")
    public ResponseEntity<ExpenseDto> updateExpense(@Parameter(description = "Expense ID") @PathVariable(name = "id") Long id, @RequestBody ExpenseDto dto) {
        return ResponseEntity.ok(expenseService.updateExpense(id, dto));
    }

    @Operation(summary = "Delete an expense")
    @ApiResponse(responseCode = "200", description = "Expense deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteExpense(@Parameter(description = "Expense ID") @PathVariable(name = "id") Long id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.ok("Expense deleted successfully");
    }

    @Operation(summary = "Get expense by ID")
    @ApiResponse(responseCode = "200", description = "Expense returned")
    @GetMapping("/{id}")
    public ResponseEntity<ExpenseDto> getExpenseById(@Parameter(description = "Expense ID") @PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(expenseService.getExpenseById(id));
    }

    @Operation(summary = "List all expenses")
    @ApiResponse(responseCode = "200", description = "Expenses returned")
    @GetMapping
    public ResponseEntity<List<ExpenseDto>> getAllExpenses() {
        return ResponseEntity.ok(expenseService.getAllExpenses());
    }
}
