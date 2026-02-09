package com.example.smartBiz.repository;

import com.example.smartBiz.entity.Business;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRepo extends JpaRepository<Business, Long> {
}
