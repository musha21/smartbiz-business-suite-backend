package com.example.smartBiz.service;

import com.example.smartBiz.dto.LandingStatDto;

import java.util.List;

public interface LandingStatService {
    LandingStatDto create(LandingStatDto dto);
    LandingStatDto update(Long id, LandingStatDto dto);
    void delete(Long id);
    LandingStatDto toggleActive(Long id);
    List<LandingStatDto> getAllForAdmin();
    List<LandingStatDto> getActiveForPublic();
}
