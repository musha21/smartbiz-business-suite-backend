package com.example.smartBiz.repository;

import com.example.smartBiz.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaleRepo extends JpaRepository<Sale,Long> {
    List<Sale> findAllByOrderBySaleDateDesc();
}
