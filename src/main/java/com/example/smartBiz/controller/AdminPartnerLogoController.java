package com.example.smartBiz.controller;

import com.example.smartBiz.dto.PartnerLogoDto;
import com.example.smartBiz.service.PartnerLogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/api/admin/partner-logos")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "Admin Partner Logos", description = "Partner logo management for admins")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPartnerLogoController {

    private final PartnerLogoService partnerLogoService;

    @Operation(summary = "Create partner logo")
    @PostMapping
    public ResponseEntity<PartnerLogoDto> create(@RequestBody PartnerLogoDto dto) {
        return ResponseEntity.ok(partnerLogoService.create(dto));
    }

    @Operation(summary = "Update partner logo")
    @PutMapping("/{id}")
    public ResponseEntity<PartnerLogoDto> update(@PathVariable Long id, @RequestBody PartnerLogoDto dto) {
        return ResponseEntity.ok(partnerLogoService.update(id, dto));
    }

    @Operation(summary = "Delete partner logo")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        partnerLogoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all partner logos for admin")
    @GetMapping
    public ResponseEntity<List<PartnerLogoDto>> getAll() {
        return ResponseEntity.ok(partnerLogoService.getAllForAdmin());
    }
}
