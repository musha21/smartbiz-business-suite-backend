package com.example.smartBiz.controller;

import com.example.smartBiz.dto.PartnerLogoDto;
import com.example.smartBiz.service.PartnerLogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/v1/api/admin/partner-logos")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "Admin Partner Logos", description = "Partner logo management for admins")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPartnerLogoController {

    private final PartnerLogoService partnerLogoService;

    @Operation(summary = "Create partner logo (multipart/form-data)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PartnerLogoDto> create(
            @RequestParam("companyName") String companyName,
            @RequestParam(value = "displayOrder", defaultValue = "0") Integer displayOrder,
            @RequestParam(value = "active", defaultValue = "true") Boolean active,
            @RequestParam("logo") MultipartFile logo
    ) throws IOException {
        return ResponseEntity.ok(partnerLogoService.create(companyName, displayOrder, active, logo));
    }

    @Operation(summary = "Update partner logo (multipart/form-data)")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PartnerLogoDto> update(
            @PathVariable Long id,
            @RequestParam("companyName") String companyName,
            @RequestParam(value = "displayOrder", required = false) Integer displayOrder,
            @RequestParam(value = "active", required = false) Boolean active,
            @RequestParam(value = "logo", required = false) MultipartFile logo
    ) throws IOException {
        return ResponseEntity.ok(partnerLogoService.update(id, companyName, displayOrder, active, logo));
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

    @Operation(summary = "Toggle partner logo active status")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<PartnerLogoDto> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(partnerLogoService.toggleActive(id));
    }
}
