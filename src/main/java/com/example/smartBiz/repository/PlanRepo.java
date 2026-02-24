package com.example.smartBiz.repository;

import com.example.smartBiz.entity.Plan;
import com.example.smartBiz.enums.PlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanRepo extends JpaRepository<Plan, Long> {

    Optional<Plan> findByCode(String code);

    List<Plan> findByActiveTrue();

    List<Plan> findAllByStatus(PlanStatus status);
}
