package com.example.smartBiz.repository;

import com.example.smartBiz.entity.Business;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BusinessRepo extends JpaRepository<Business, Long> {

    @Query("SELECT COUNT(b) FROM Business b WHERE b.active = true")
    Long countActiveBusinesses();

    @Query("SELECT b.name FROM Business b WHERE b.id = :id")
    Optional<String> findNameById(@Param("id") Long id);
}
