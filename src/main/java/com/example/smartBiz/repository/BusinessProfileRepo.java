package com.example.smartBiz.repository;

import com.example.smartBiz.entity.BusinessProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BusinessProfileRepo extends JpaRepository<BusinessProfile, Long> {
    Optional<BusinessProfile> findByBusinessId(Long businessId);
}
