package com.example.smartBiz.repository;

import com.example.smartBiz.entity.Business;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessRepo extends JpaRepository<Business, Long> {

    @Query("SELECT COUNT(b) FROM Business b WHERE b.active = true")
    Long countActiveBusinesses();

    @Query("SELECT b.name FROM Business b WHERE b.id = :id")
    Optional<String> findNameById(@Param("id") Long id);

    @Query("SELECT COUNT(b) FROM Business b WHERE b.createdAt BETWEEN :start AND :end")
    Long countByCreatedAtBetween(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);

    @Query("SELECT p.name, (SELECT COUNT(b) FROM Business b WHERE b.active = true AND TRIM(LOWER(b.plan)) = TRIM(LOWER(p.name))) FROM Plan p WHERE p.active = true")
    List<Object[]> countBusinessesGroupByPlan();

    List<Business> findByPlanIsNull();
}
