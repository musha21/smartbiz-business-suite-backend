package com.example.smartBiz.repository;

import com.example.smartBiz.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ExpenseRepo extends JpaRepository<Expense, Long> {
    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM Expense e
        WHERE e.expenseDate BETWEEN :start AND :end
    """)
    Double sumExpensesBetween(LocalDateTime start, LocalDateTime end);
    List<Expense> findByBusinessId(Long businessId);

}
