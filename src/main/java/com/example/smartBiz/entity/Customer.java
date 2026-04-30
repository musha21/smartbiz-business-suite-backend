package com.example.smartBiz.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "customers", indexes = {
        @Index(name = "idx_customer_created_at",
                columnList = "created_at"),               // ✅ snake_case
        @Index(name = "idx_customer_business",
                columnList = "business_id")               // ✅ snake_case
})
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Customer name is required")     // ✅ validation
    private String name;

    @Column(unique = true)
    @Email(message = "Invalid email format")             // ✅ validation
    private String email;

    @Pattern(
            regexp = "^\\+?[0-9]{7,15}$",
            message = "Invalid phone number"
    )
    private String phone;                                // ✅ validation

    private String address;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "archived", nullable = false)
    private Boolean archived = false;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;                     // ✅ clean import

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;                     // ✅ new — track updates
    private boolean isDeleted;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();            // ✅ set on create too
        this.archived = false;                           // ✅ safety default
        this.isDeleted = false; // ✅ IMPORTANT
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();            // ✅ auto update timestamp
    }
}