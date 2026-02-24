package com.example.smartBiz.repository;

import com.example.smartBiz.entity.Business;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BusinessRepo extends JpaRepository<Business, Long> {

    @Query("SELECT COUNT(b) FROM Business b WHERE b.active = true")
    Long countActiveBusinesses();
}
