package com.example.smartBiz.repository;

import com.example.smartBiz.entity.HeroContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HeroContentRepo extends JpaRepository<HeroContent, Long> {
    Optional<HeroContent> findFirstByOrderByIdAsc();
}
