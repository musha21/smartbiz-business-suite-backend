
package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.CategoryDto;
import com.example.smartBiz.entity.Category;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.CategoryRepo;
import com.example.smartBiz.security.CustomUserPrincipal;
import com.example.smartBiz.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepo categoryRepo;

    public CategoryServiceImpl(CategoryRepo categoryRepo) {
        this.categoryRepo = categoryRepo;
    }

    private Long requireBusinessId() {
        CustomUserPrincipal principal = CustomUserPrincipal.getCurrent();
        if (principal == null || principal.getBusinessId() == null)
            throw new ResourceNotFoundException("Business context missing (JWT token required)");
        return principal.getBusinessId();
    }

    @Override
    public CategoryDto create(CategoryDto dto) {
        Long businessId = requireBusinessId();

        String name = dto.getName() == null ? "" : dto.getName().trim();
        if (name.isEmpty())
            throw new ResourceNotFoundException("Category name is required");

        if (categoryRepo.existsByBusinessIdAndNameIgnoreCase(businessId, name)) {
            throw new ResourceNotFoundException("Category already exists");
        }

        Category c = new Category();
        c.setBusinessId(businessId);
        c.setName(name);

        Category saved = categoryRepo.save(c);
        return new CategoryDto(saved.getId(), saved.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> list() {
        Long businessId = requireBusinessId();

        return categoryRepo.findByBusinessIdOrderByNameAsc(businessId)
                .stream()
                .map(c -> new CategoryDto(c.getId(), c.getName()))
                .toList();
    }
}