package com.example.smartBiz.service;

import com.example.smartBiz.dto.TestimonialRequestDto;
import com.example.smartBiz.dto.TestimonialResponseDto;

import java.util.List;

public interface TestimonialService {
    TestimonialResponseDto create(TestimonialRequestDto dto);
    TestimonialResponseDto update(Long id, TestimonialRequestDto dto);
    void delete(Long id);
    List<TestimonialResponseDto> getAllForAdmin();
    List<TestimonialResponseDto> getActiveForPublic();
}
