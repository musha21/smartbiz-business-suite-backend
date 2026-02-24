package com.example.smartBiz.repository;

import com.example.smartBiz.entity.PlanLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanLimitRepo extends JpaRepository<PlanLimit, Long> {

    List<PlanLimit> findByPlanId(Long planId);

    Optional<PlanLimit> findByPlanIdAndLimitKey(Long planId, String limitKey);

    void deleteByPlanId(Long planId);
}
