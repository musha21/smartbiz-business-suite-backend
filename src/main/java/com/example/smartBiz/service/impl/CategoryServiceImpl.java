
package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.CategoryDto;
import com.example.smartBiz.entity.Category;
import com.example.smartBiz.repository.CategoryRepo;
import com.example.smartBiz.security.RequestContext;
import com.example.smartBiz.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepo categoryRepo;
    private final RequestContext requestContext;

    public CategoryServiceImpl(CategoryRepo categoryRepo, RequestContext requestContext) {
        this.categoryRepo = categoryRepo;
        this.requestContext = requestContext;
    }

    private Long requireBusinessId() {
        Long businessId = requestContext.getBusinessId();
        if (businessId == null) throw new RuntimeException("Business context missing (JWT token required)");
        return businessId;
    }

    @Override
    public CategoryDto create(CategoryDto dto) {
        Long businessId = requireBusinessId();

        String name = dto.getName() == null ? "" : dto.getName().trim();
        if (name.isEmpty()) throw new RuntimeException("Category name is required");

        if (categoryRepo.existsByBusinessIdAndNameIgnoreCase(businessId, name)) {
            throw new RuntimeException("Category already exists");
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