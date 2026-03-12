package com.example.smartBiz.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
@Entity
@Table(name = "customers", indexes = {
        @Index(name = "idx_customer_created_at", columnList = "createdAt"),
        @Index(name = "idx_customer_business", columnList = "businessId")
})
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String address;
    @Column(nullable = false)
    private Long businessId;

    private java.time.LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = java.time.LocalDateTime.now();
    }
}
