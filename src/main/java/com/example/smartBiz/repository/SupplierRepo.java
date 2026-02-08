package com.example.smartBiz.repository;

import com.example.smartBiz.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierRepo extends JpaRepository<Supplier,Long> {
    List<Supplier> findByBusinessId(Long businessId);
}
