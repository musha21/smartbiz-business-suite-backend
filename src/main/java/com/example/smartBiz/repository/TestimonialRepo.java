package com.example.smartBiz.repository;

import com.example.smartBiz.entity.Testimonial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestimonialRepo extends JpaRepository<Testimonial, Long> {
    List<Testimonial> findAllByActiveTrueOrderByCreatedAtDesc();
}
