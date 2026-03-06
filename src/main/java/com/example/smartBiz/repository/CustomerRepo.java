package com.example.smartBiz.repository;

import com.example.smartBiz.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerRepo extends JpaRepository<Customer, Long> {

    List<Customer> findByBusinessId(Long businessId);

    long countByBusinessId(Long businessId);
}
