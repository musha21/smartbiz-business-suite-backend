package com.example.smartBiz.controller;

import com.example.smartBiz.dto.LandingStatDto;
import com.example.smartBiz.service.LandingStatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/api/public/stats")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "Public Landing Stats", description = "Public landing stats fetch")
public class PublicLandingStatController {

    private final LandingStatService landingStatService;

    @Operation(summary = "Get all active landing stats")
    @GetMapping
    public ResponseEntity<List<LandingStatDto>> getActiveStats() {
        return ResponseEntity.ok(landingStatService.getActiveForPublic());
    }
}
