package com.example.smartBiz.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "suppliers")
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String phone;
    private String address;
    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL)
    private List<Products> products;
    @Column(nullable = false)
    private Long businessId;

    @Column(name = "archived", nullable = false)
    private Boolean archived = false;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "payment_terms", length = 20)
    private String paymentTerms = "NET_30";

    @Column(name = "lead_time_days")
    private Integer leadTimeDays = 7;

    @Column(name = "moq")
    private Integer moq = 1;

    @Column(name = "reliability_score", precision = 3, scale = 2)
    private BigDecimal reliabilityScore = BigDecimal.valueOf(1.00);

    @Column(name = "late_delivery_count")
    private Integer lateDeliveryCount = 0;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Column(name = "bank_account", length = 100)
    private String bankAccount;

    @Column(name = "currency", length = 3)
    private String currency = "LKR";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public Supplier(String name, String email, String phone, String address) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
    }


}


