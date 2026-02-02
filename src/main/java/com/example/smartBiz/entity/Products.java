package com.example.smartBiz.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
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
        private Integer stockQty;
        private Integer lowStockLimit;
        private Long businessId;

        public Products(String name, Double price, Integer stockQty, Integer lowStockLimit, Long businessId) {
                this.name = name;
                this.price = price;
                this.stockQty = stockQty;
                this.lowStockLimit = lowStockLimit;
                this.businessId = businessId;
        }
    }
