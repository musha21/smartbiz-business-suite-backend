package com.example.smartBiz.repository;

import com.example.smartBiz.entity.BusinessSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BusinessSequenceRepo extends JpaRepository<BusinessSequence, Long> {
    Optional<BusinessSequence> findByBusinessIdAndSequenceType(Long businessId, String sequenceType);
}
