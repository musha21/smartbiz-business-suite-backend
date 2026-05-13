package com.example.smartBiz.controller;

import com.example.smartBiz.dto.TrustIntegrationDto;
import com.example.smartBiz.service.TrustIntegrationService;
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
@RequestMapping("/v1/api/public/integrations")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "Public Trust Integrations", description = "Public trust integration fetch")
public class PublicTrustIntegrationController {

    private final TrustIntegrationService trustIntegrationService;

    @Operation(summary = "Get all active trust integrations")
    @GetMapping
    public ResponseEntity<List<TrustIntegrationDto>> getActiveIntegrations() {
        return ResponseEntity.ok(trustIntegrationService.getActiveForPublic());
    }
}
