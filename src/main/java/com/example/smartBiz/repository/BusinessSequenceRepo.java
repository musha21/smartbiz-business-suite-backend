package com.example.smartBiz.repository;

import com.example.smartBiz.entity.BusinessSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BusinessSequenceRepo extends JpaRepository<BusinessSequence, Long> {
    Optional<BusinessSequence> findByBusinessIdAndSequenceType(Long businessId, String sequenceType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM BusinessSequence s WHERE s.businessId = :businessId AND s.sequenceType = :sequenceType AND s.fiscalYear = :fiscalYear")
    Optional<BusinessSequence> findByBusinessIdAndSequenceTypeAndFiscalYearForUpdate(
            @Param("businessId") Long businessId,
            @Param("sequenceType") String sequenceType,
            @Param("fiscalYear") Integer fiscalYear);
}
