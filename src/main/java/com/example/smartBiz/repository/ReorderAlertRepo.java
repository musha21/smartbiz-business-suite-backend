package com.example.smartBiz.repository;

import com.example.smartBiz.entity.ReorderAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReorderAlertRepo extends JpaRepository<ReorderAlert, Long> {

    List<ReorderAlert> findByBusinessIdAndStatusOrderByTriggeredAtDesc(Long businessId, ReorderAlert.AlertStatus status);

    List<ReorderAlert> findByBusinessIdOrderByTriggeredAtDesc(Long businessId);

    Optional<ReorderAlert> findByIdAndBusinessId(Long id, Long businessId);

    boolean existsByProduct_IdAndBatch_IdAndStatus(Long productId, Long batchId, ReorderAlert.AlertStatus status);

    boolean existsByProduct_IdAndStatus(Long productId, ReorderAlert.AlertStatus status);

    Long countByBusinessIdAndStatus(Long businessId, ReorderAlert.AlertStatus status);

    @Query("""
            SELECT ra FROM ReorderAlert ra
            JOIN FETCH ra.product
            JOIN FETCH ra.suggestedSupplier
            WHERE ra.businessId = :businessId AND ra.status = 'ACTIVE'
            ORDER BY ra.triggeredAt DESC
            """)
    List<ReorderAlert> findActiveAlertsWithDetails(@Param("businessId") Long businessId);

    @Query("""
            SELECT ra FROM ReorderAlert ra
            WHERE ra.businessId = :businessId
            AND ra.status = 'ACTIVE'
            AND ra.triggeredAt < :beforeDate
            """)
    List<ReorderAlert> findStaleAlerts(
            @Param("businessId") Long businessId,
            @Param("beforeDate") LocalDateTime beforeDate);

    @Query("""
            SELECT COUNT(ra) FROM ReorderAlert ra
            WHERE ra.businessId = :businessId AND ra.status = 'ACTIVE'
            """)
    Long countActiveByBusinessId(@Param("businessId") Long businessId);
}
