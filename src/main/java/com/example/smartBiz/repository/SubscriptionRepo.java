package com.example.smartBiz.repository;

import com.example.smartBiz.entity.Subscription;
import com.example.smartBiz.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepo extends JpaRepository<Subscription, Long> {

    /** Find the active subscription for a business */
    Optional<Subscription> findByBusinessIdAndStatus(Long businessId, SubscriptionStatus status);

    /** Find all subscriptions for a business (history) */
    List<Subscription> findByBusinessIdOrderByCreatedAtDesc(Long businessId);

    Optional<Subscription> findFirstByBusinessIdOrderByCreatedAtDesc(Long businessId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(s) FROM Subscription s WHERE s.plan.monthlyPrice = 0 AND s.status = 'ACTIVE'")
    Long countActiveFreeSubscriptions();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(s) FROM Subscription s WHERE s.plan.monthlyPrice > 0 AND s.status = 'ACTIVE'")
    Long countActivePaidSubscriptions();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(s) FROM Subscription s WHERE s.status = 'ACTIVE' AND s.endAt BETWEEN :now AND :soon")
    Long countExpiringSoon(@org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now,
            @org.springframework.data.repository.query.Param("soon") java.time.LocalDateTime soon);

    Long countByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("""
                SELECT new com.example.smartBiz.dto.ExpiringSubscriptionDTO(
                    b.name, p.name, s.endAt, s.status
                )
                FROM Subscription s
                JOIN s.business b
                JOIN s.plan p
                WHERE s.endAt BETWEEN :now AND :soon
                ORDER BY s.endAt ASC
            """)
    List<com.example.smartBiz.dto.ExpiringSubscriptionDTO> findExpiringSubscriptions(
            @org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now,
            @org.springframework.data.repository.query.Param("soon") java.time.LocalDateTime soon);
}
