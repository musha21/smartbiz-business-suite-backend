package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ExpenseDto {
    private Long id; // optional for response
    private LocalDateTime expenseDate;
    private String category;
    private Double amount;
    private String note;
}
