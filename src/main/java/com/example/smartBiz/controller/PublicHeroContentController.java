package com.example.smartBiz.controller;

import com.example.smartBiz.dto.HeroContentDto;
import com.example.smartBiz.service.HeroContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/api/public/hero")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "Public Hero Content", description = "Public hero content fetch")
public class PublicHeroContentController {

    private final HeroContentService heroContentService;

    @Operation(summary = "Get public hero content")
    @GetMapping
    public ResponseEntity<HeroContentDto> getContent() {
        return ResponseEntity.ok(heroContentService.getContent());
    }
}
