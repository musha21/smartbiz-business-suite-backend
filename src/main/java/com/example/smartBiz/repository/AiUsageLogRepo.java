package com.example.smartBiz.repository;

import com.example.smartBiz.entity.AiUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AiUsageLogRepo extends JpaRepository<AiUsageLog, Long> {

    @Query("SELECT SUM(a.creditsUsed) FROM AiUsageLog a WHERE a.timestamp BETWEEN :start AND :end")
    Long sumTotalUsageBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT DAYNAME(a.timestamp), SUM(a.creditsUsed) FROM AiUsageLog a WHERE a.timestamp BETWEEN :start AND :end GROUP BY DAYNAME(a.timestamp)")
    List<Object[]> sumUsageGroupByDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT u.name, SUM(a.creditsUsed) FROM AiUsageLog a JOIN AppUser u ON a.userId = u.id WHERE a.timestamp BETWEEN :start AND :end GROUP BY u.name ORDER BY SUM(a.creditsUsed) DESC")
    List<Object[]> sumUsageGroupByUser(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT b.name, SUM(a.creditsUsed) FROM AiUsageLog a JOIN Business b ON a.businessId = b.id WHERE a.timestamp BETWEEN :start AND :end GROUP BY b.name")
    List<Object[]> sumUsageGroupByBusiness(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
