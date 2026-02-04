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
        private Integer stock_qty;
        private Integer low_stock_limit;

        public Products(String name, Double price, Integer stock_qty, Integer low_stock_limit, Long business_id) {
                this.name = name;
                this.price = price;
                this.stock_qty = stock_qty;
                this.low_stock_limit = low_stock_limit;
                this.business_id = business_id;
        }

        private Long  business_id ;



}
