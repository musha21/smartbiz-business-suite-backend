package com.example.smartBiz.repository;

import com.example.smartBiz.entity.UsageCounter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsageCounterRepo extends JpaRepository<UsageCounter, Long> {

    Optional<UsageCounter> findByBusinessIdAndYearMonth(Long businessId, String yearMonth);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(u.aiCount) FROM UsageCounter u WHERE u.yearMonth = :yearMonth")
    Long sumTotalAiCountByYearMonth(@org.springframework.data.repository.query.Param("yearMonth") String yearMonth);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(u.aiCount) FROM UsageCounter u")
    Long sumTotalAiCount();

    @org.springframework.data.jpa.repository.Query("SELECT b.name, SUM(u.aiCount) FROM UsageCounter u JOIN Business b ON u.businessId = b.id GROUP BY b.name")
    List<Object[]> sumAiCountGroupByBusiness();
}
