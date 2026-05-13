package com.example.smartBiz.service;

import com.example.smartBiz.dto.FAQDto;

import java.util.List;

public interface FAQService {
    FAQDto create(FAQDto dto);
    FAQDto update(Long id, FAQDto dto);
    void delete(Long id);
    FAQDto toggleActive(Long id);
    List<FAQDto> getAllForAdmin();
    List<FAQDto> getActiveForPublic();
}
