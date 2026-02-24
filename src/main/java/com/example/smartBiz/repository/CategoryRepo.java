
package com.example.smartBiz.repository;

import com.example.smartBiz.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepo extends JpaRepository<Category, Long> {

    List<Category> findByBusinessIdOrderByNameAsc(Long businessId);

    Optional<Category> findByIdAndBusinessId(Long id, Long businessId);

    boolean existsByBusinessIdAndNameIgnoreCase(Long businessId, String name);
}