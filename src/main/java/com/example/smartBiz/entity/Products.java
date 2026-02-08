package com.example.smartBiz.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "products")
public class Products {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Double price;
    private Integer stock_qty;
    private Integer low_stock_limit;
    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    public Products(String name, Double price, Integer stock_qty, Integer low_stock_limit, Long businessId) {
        this.name = name;
        this.price = price;
        this.stock_qty = stock_qty;
        this.low_stock_limit = low_stock_limit;
        this.businessId = businessId;

    }


}
