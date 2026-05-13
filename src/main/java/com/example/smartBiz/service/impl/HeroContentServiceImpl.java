package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.HeroContentDto;
import com.example.smartBiz.entity.HeroContent;
import com.example.smartBiz.repository.HeroContentRepo;
import com.example.smartBiz.service.HeroContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HeroContentServiceImpl implements HeroContentService {

    private final HeroContentRepo heroContentRepo;

    @Override
    public HeroContentDto getContent() {
        HeroContent content = heroContentRepo.findFirstByOrderByIdAsc()
                .orElseGet(this::createDefault);
        return mapToDto(content);
    }

    @Override
    @Transactional
    public HeroContentDto updateContent(HeroContentDto dto) {
        HeroContent content = heroContentRepo.findFirstByOrderByIdAsc()
                .orElseGet(() -> {
                    HeroContent newContent = new HeroContent();
                    return heroContentRepo.save(newContent);
                });
        
        content.setBadgeText(dto.getBadgeText());
        content.setHeadline(dto.getHeadline());
        content.setSubheadline(dto.getSubheadline());
        content.setCtaText(dto.getCtaText());
        
        return mapToDto(heroContentRepo.save(content));
    }

    private HeroContent createDefault() {
        HeroContent content = HeroContent.builder()
                .badgeText("Your Smart POS software solution")
                .headline("The Future of Business Management")
                .subheadline("Everything you need to run your store smoothly, in one smart platform.")
                .ctaText("Get Free Demo")
                .build();
        return heroContentRepo.save(content);
    }

    private HeroContentDto mapToDto(HeroContent content) {
        return HeroContentDto.builder()
                .id(content.getId())
                .badgeText(content.getBadgeText())
                .headline(content.getHeadline())
                .subheadline(content.getSubheadline())
                .ctaText(content.getCtaText())
                .updatedAt(content.getUpdatedAt())
                .build();
    }
}
