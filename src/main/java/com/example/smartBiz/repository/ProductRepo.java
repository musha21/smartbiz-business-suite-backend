package com.example.smartBiz.repository;

import com.example.smartBiz.entity.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepo extends JpaRepository<Products, Long> {
    @Query("SELECT prod FROM Products prod WHERE prod.stock_qty <= prod.low_stock_limit")
    List<Products> findLowStockProducts();
}





