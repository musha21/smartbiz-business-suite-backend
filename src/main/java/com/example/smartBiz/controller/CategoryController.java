package com.example.smartBiz.controller;

import com.example.smartBiz.dto.CategoryDto;
import com.example.smartBiz.entity.Category;
import com.example.smartBiz.repository.CategoryRepo;
import com.example.smartBiz.security.RequestContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/api/categories")
public class CategoryController {

    private final CategoryRepo categoryRepo;
    private final RequestContext requestContext;

    public CategoryController(CategoryRepo categoryRepo, RequestContext requestContext) {
        this.categoryRepo = categoryRepo;
        this.requestContext = requestContext;
    }

    private Long requireBusinessId() {
        Long businessId = requestContext.getBusinessId();
        if (businessId == null) throw new RuntimeException("Business context missing (JWT token required)");
        return businessId;
    }

    @PostMapping
    public ResponseEntity<CategoryDto> create(@RequestBody CategoryDto dto) {
        Long businessId = requireBusinessId();

        String name = dto.getName() == null ? "" : dto.getName().trim();
        if (name.isEmpty()) throw new RuntimeException("Category name is required");

        if (categoryRepo.existsByBusinessIdAndName(businessId, name)) {
            throw new RuntimeException("Category already exists");
        }

        Category c = new Category();
        c.setBusinessId(businessId);
        c.setName(name);

        Category saved = categoryRepo.save(c);
        return ResponseEntity.ok(new CategoryDto(saved.getId(), saved.getName()));
    }

    @GetMapping
    public ResponseEntity<List<CategoryDto>> list() {
        Long businessId = requireBusinessId();

        List<CategoryDto> out = categoryRepo.findByBusinessId(businessId)
                .stream()
                .map(c -> new CategoryDto(c.getId(), c.getName()))
                .toList();

        return ResponseEntity.ok(out);
    }
}
