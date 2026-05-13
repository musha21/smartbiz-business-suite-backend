package com.example.smartBiz.controller;

import com.example.smartBiz.dto.PartnerLogoDto;
import com.example.smartBiz.service.PartnerLogoService;
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
@RequestMapping("/v1/api/partner-logos")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "Public Partner Logos", description = "Public partner logo fetch for landing page")
public class PublicPartnerLogoController {

    private final PartnerLogoService partnerLogoService;

    @Operation(summary = "Get all active partner logos")
    @GetMapping
    public ResponseEntity<List<PartnerLogoDto>> getActiveLogos() {
        return ResponseEntity.ok(partnerLogoService.getActiveForPublic());
    }
}
