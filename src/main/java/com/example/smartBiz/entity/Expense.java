package com.example.smartBiz.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "expenses", indexes = {
        @Index(name = "idx_expenses_business_date", columnList = "businessId, expenseDate")
})
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime expenseDate;

    private String category; // e.g. Rent, Salary, Transport
    private Double amount;
    private String note;
    // optional description
    @Column(nullable = false)
    private Long businessId;

    @Column(name = "cash_register_session_id")
    private Long cashRegisterSessionId;
}
