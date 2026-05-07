package com.example.smartBiz.controller;

import com.example.smartBiz.dto.BusinessProfileDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.smartBiz.security.CustomUserPrincipal;
import com.example.smartBiz.service.BusinessProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/api/business/profile")
@CrossOrigin
@Tag(name = "Business Profile", description = "Get/create/update business profile")
public class BusinessProfileController {

    private final BusinessProfileService profileService;

    public BusinessProfileController(BusinessProfileService profileService) {
        this.profileService = profileService;
    }

    @Operation(summary = "Get business profile", description = "Returns the profile (name, address, logo, tax info) for the authenticated user's business.")
    @ApiResponse(responseCode = "200", description = "Profile returned")
    @GetMapping
    public ResponseEntity<BusinessProfileDto> getProfile() {
        Long businessId = CustomUserPrincipal.getCurrent().getBusinessId();
        return ResponseEntity.ok(profileService.getProfile(businessId));
    }

    @Operation(summary = "Create business profile", description = "Creates a new profile for the authenticated user's business. Only one profile per business.")
    @ApiResponse(responseCode = "200", description = "Profile created")
    @PostMapping
    public ResponseEntity<BusinessProfileDto> createProfile(@RequestBody BusinessProfileDto dto) {
        Long businessId = CustomUserPrincipal.getCurrent().getBusinessId();
        return ResponseEntity.ok(profileService.createProfile(businessId, dto));
    }

    @Operation(summary = "Update business profile")
    @ApiResponse(responseCode = "200", description = "Profile updated")
    @PutMapping
    public ResponseEntity<BusinessProfileDto> updateProfile(@RequestBody BusinessProfileDto dto) {
        Long businessId = CustomUserPrincipal.getCurrent().getBusinessId();
        return ResponseEntity.ok(profileService.updateProfile(businessId, dto));
    }
}
