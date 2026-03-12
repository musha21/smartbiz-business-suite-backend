package com.example.smartBiz.repository;

import com.example.smartBiz.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerRepo extends JpaRepository<Customer, Long> {

    List<Customer> findByBusinessId(Long businessId);

    long countByBusinessId(Long businessId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(c) FROM Customer c WHERE c.createdAt BETWEEN :start AND :end")
    long countByCreatedAtBetween(@org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start, @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);
}
