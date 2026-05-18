package com.example.smartBiz.repository;

import com.example.smartBiz.entity.CashRegisterSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CashRegisterSessionRepo extends JpaRepository<CashRegisterSession, Long> {

    Optional<CashRegisterSession> findByBusinessIdAndStatus(Long businessId, CashRegisterSession.SessionStatus status);

    @Query("SELECT s FROM CashRegisterSession s WHERE s.businessId = :businessId AND s.openTime >= :startOfDay AND s.openTime < :endOfDay")
    List<CashRegisterSession> findByBusinessIdAndDate(
            @Param("businessId") Long businessId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);

    Page<CashRegisterSession> findByBusinessIdOrderByOpenTimeDesc(Long businessId, Pageable pageable);

    @Query("""
            SELECT s FROM CashRegisterSession s
            WHERE s.businessId = :businessId
            AND s.openTime BETWEEN :startDate AND :endDate
            ORDER BY s.openTime DESC
            """)
    List<CashRegisterSession> findSessionsByDateRange(
            @Param("businessId") Long businessId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    boolean existsByBusinessIdAndStatus(Long businessId, CashRegisterSession.SessionStatus status);
}
