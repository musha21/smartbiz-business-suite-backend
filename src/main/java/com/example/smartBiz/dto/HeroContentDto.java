package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeroContentDto {
    private Long id;
    private String badgeText;
    private String headline;
    private String subheadline;
    private String ctaText;
    private LocalDateTime updatedAt;
}
