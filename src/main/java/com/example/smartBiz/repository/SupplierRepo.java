package com.example.smartBiz.repository;

import com.example.smartBiz.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierRepo extends JpaRepository<Supplier, Long> {
    List<Supplier> findByBusinessId(Long businessId);

    Optional<Supplier> findByIdAndBusinessId(Long id, Long businessId);
}
