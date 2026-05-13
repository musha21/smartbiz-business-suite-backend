package com.example.smartBiz.service;

import com.example.smartBiz.dto.HeroContentDto;

public interface HeroContentService {
    HeroContentDto getContent();
    HeroContentDto updateContent(HeroContentDto dto);
}
