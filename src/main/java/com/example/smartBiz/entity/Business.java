package com.example.smartBiz.entity;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "businesses", indexes = {
        @Index(name = "idx_business_created_at", columnList = "createdAt")
})
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Boolean active = true;

    private Long planId;
    private String plan;
    private java.time.LocalDateTime subscriptionStart;
    private java.time.LocalDateTime subscriptionEnd;

    private java.time.LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = java.time.LocalDateTime.now();
    }
}
