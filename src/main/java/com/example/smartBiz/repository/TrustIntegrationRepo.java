package com.example.smartBiz.repository;

import com.example.smartBiz.entity.TrustIntegration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrustIntegrationRepo extends JpaRepository<TrustIntegration, Long> {
    List<TrustIntegration> findAllByActiveTrueOrderByDisplayOrderAsc();
    List<TrustIntegration> findAllByOrderByDisplayOrderAsc();
}
