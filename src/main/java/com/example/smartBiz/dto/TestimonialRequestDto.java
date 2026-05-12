package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestimonialRequestDto {
    private String authorName;
    private String authorRole;
    private String authorCompany;
    private String content;
    private Integer rating;
    private String avatarUrl;
    private Boolean active;
}
