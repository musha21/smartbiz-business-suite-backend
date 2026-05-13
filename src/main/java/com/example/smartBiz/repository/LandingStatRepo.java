package com.example.smartBiz.repository;

import com.example.smartBiz.entity.LandingStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LandingStatRepo extends JpaRepository<LandingStat, Long> {
    List<LandingStat> findAllByActiveTrueOrderByDisplayOrderAsc();
    List<LandingStat> findAllByOrderByDisplayOrderAsc();
}
