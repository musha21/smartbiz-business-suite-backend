package com.example.smartBiz.repository;

import com.example.smartBiz.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ExpenseRepo extends JpaRepository<Expense, Long> {

  // ✅ List expenses for a business
  List<Expense> findByBusinessId(Long businessId);

  long countByBusinessId(Long businessId);

  // ✅ Sum expenses for a business between dates
  @Query("""
          SELECT COALESCE(SUM(e.amount), 0)
          FROM Expense e
          WHERE e.businessId = :businessId
            AND e.expenseDate BETWEEN :start AND :end
      """)
  Double sumExpensesByBusinessBetween(
      @Param("businessId") Long businessId,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);
}
