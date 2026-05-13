package com.example.smartBiz.controller;

import com.example.smartBiz.dto.HeroContentDto;
import com.example.smartBiz.service.HeroContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/api/admin/hero")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "Admin Hero Content", description = "Hero content management")
@PreAuthorize("hasRole('ADMIN')")
public class AdminHeroContentController {

    private final HeroContentService heroContentService;

    @Operation(summary = "Get hero content")
    @GetMapping
    public ResponseEntity<HeroContentDto> getContent() {
        return ResponseEntity.ok(heroContentService.getContent());
    }

    @Operation(summary = "Update hero content")
    @PutMapping
    public ResponseEntity<HeroContentDto> updateContent(@RequestBody HeroContentDto dto) {
        return ResponseEntity.ok(heroContentService.updateContent(dto));
    }
}
