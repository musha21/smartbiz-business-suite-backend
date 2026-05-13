package com.example.smartBiz.repository;

import com.example.smartBiz.entity.FAQ;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FAQRepo extends JpaRepository<FAQ, Long> {
    List<FAQ> findAllByActiveTrueOrderByDisplayOrderAsc();
    List<FAQ> findAllByOrderByDisplayOrderAsc();
}
