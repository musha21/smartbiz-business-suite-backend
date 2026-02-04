package com.example.smartBiz.repository;

import com.example.smartBiz.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepo extends JpaRepository<Customer,Long> {


}
