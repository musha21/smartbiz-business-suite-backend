package com.example.smartBiz.controller;

import com.example.smartBiz.dto.BusinessProfileDto;
import com.example.smartBiz.security.CustomUserPrincipal;
import com.example.smartBiz.service.BusinessProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/api/business/profile")
@CrossOrigin
public class BusinessProfileController {

    private final BusinessProfileService profileService;

    public BusinessProfileController(BusinessProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<BusinessProfileDto> getProfile() {
        Long businessId = CustomUserPrincipal.getCurrent().getBusinessId();
        return ResponseEntity.ok(profileService.getProfile(businessId));
    }

    @PostMapping
    public ResponseEntity<BusinessProfileDto> createProfile(@RequestBody BusinessProfileDto dto) {
        Long businessId = CustomUserPrincipal.getCurrent().getBusinessId();
        return ResponseEntity.ok(profileService.createProfile(businessId, dto));
    }

    @PutMapping
    public ResponseEntity<BusinessProfileDto> updateProfile(@RequestBody BusinessProfileDto dto) {
        Long businessId = CustomUserPrincipal.getCurrent().getBusinessId();
        return ResponseEntity.ok(profileService.updateProfile(businessId, dto));
    }
}
