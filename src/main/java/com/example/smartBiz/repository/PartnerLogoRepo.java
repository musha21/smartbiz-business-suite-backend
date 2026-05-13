package com.example.smartBiz.repository;

import com.example.smartBiz.entity.PartnerLogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartnerLogoRepo extends JpaRepository<PartnerLogo, Long> {
    List<PartnerLogo> findAllByActiveTrueOrderByDisplayOrderAsc();
}
