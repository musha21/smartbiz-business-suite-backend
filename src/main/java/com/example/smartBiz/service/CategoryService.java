package com.example.smartBiz.service;

import com.example.smartBiz.dto.CategoryDto;

import java.util.List;

public interface CategoryService {
    CategoryDto create(CategoryDto dto);
    List<CategoryDto> list();
}