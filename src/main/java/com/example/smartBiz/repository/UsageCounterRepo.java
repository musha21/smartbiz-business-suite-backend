package com.example.smartBiz.repository;

import com.example.smartBiz.entity.UsageCounter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsageCounterRepo extends JpaRepository<UsageCounter, Long> {

    Optional<UsageCounter> findByBusinessIdAndYearMonth(Long businessId, String yearMonth);
}
