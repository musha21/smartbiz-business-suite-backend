package com.example.smartBiz.repository;

import com.example.smartBiz.entity.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepo extends JpaRepository<Products, Long> {


    @Query("SELECT COUNT(product) FROM Products product WHERE product.stock_qty <= product.low_stock_limit")
    long countLowStockProducts();
}





