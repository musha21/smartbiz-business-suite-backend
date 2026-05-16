package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.BusinessProfileDto;
import com.example.smartBiz.entity.BusinessProfile;
import com.example.smartBiz.repository.BusinessProfileRepo;
import com.example.smartBiz.repository.BusinessRepo;
import com.example.smartBiz.service.BusinessProfileService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

@Service
public class BusinessProfileServiceImpl implements BusinessProfileService {

    private final BusinessProfileRepo profileRepo;
    private final BusinessRepo businessRepo;

    public BusinessProfileServiceImpl(BusinessProfileRepo profileRepo, BusinessRepo businessRepo) {
        this.profileRepo = profileRepo;
        this.businessRepo = businessRepo;
    }

    @Override
    public BusinessProfileDto getProfile(Long businessId) {

        return profileRepo.findByBusinessId(businessId)
                .map(this::toDto)
                .orElseGet(() -> {
                    String businessName = businessRepo.findNameById(businessId)
                            .orElse("My Business");

                    return BusinessProfileDto.builder()
                            .businessName(businessName)
                            .currency("LKR")
                            .build();
                });
    }

    @Override
    public BusinessProfileDto createProfile(Long businessId, BusinessProfileDto dto) {

        // Upsert: if profile exists, update it instead of throwing error
        BusinessProfile profile = profileRepo.findByBusinessId(businessId)
                .orElseGet(() -> BusinessProfile.builder().businessId(businessId).build());

        profile.setBusinessName(dto.getBusinessName());
        profile.setOwnerName(dto.getOwnerName());
        profile.setLogo(dto.getLogo());
        profile.setEmail(dto.getEmail());
        profile.setPhone(dto.getPhone());
        profile.setAddress(dto.getAddress());
        profile.setCurrency(dto.getCurrency());
        profile.setInvoicePrefix(dto.getInvoicePrefix());
        profile.setBrandColor(dto.getBrandColor());
        profile.setIndustry(dto.getIndustry());
        profile.setCountry(dto.getCountry());
        profile.setBrandTagline(dto.getBrandTagline());

        return toDto(profileRepo.save(profile));
    }

    @Override
    public BusinessProfileDto updateProfile(Long businessId, BusinessProfileDto dto) {

        // Auto-create profile if it doesn't exist
        BusinessProfile profile = profileRepo.findByBusinessId(businessId)
                .orElseGet(() -> BusinessProfile.builder()
                        .businessId(businessId)
                        .build());

        profile.setBusinessName(dto.getBusinessName());
        profile.setOwnerName(dto.getOwnerName());
        profile.setLogo(dto.getLogo());
        profile.setEmail(dto.getEmail());
        profile.setPhone(dto.getPhone());
        profile.setAddress(dto.getAddress());
        profile.setCurrency(dto.getCurrency());
        profile.setInvoicePrefix(dto.getInvoicePrefix());
        profile.setBrandColor(dto.getBrandColor());
        profile.setIndustry(dto.getIndustry());
        profile.setCountry(dto.getCountry());
        profile.setBrandTagline(dto.getBrandTagline());

        BusinessProfile saved = profileRepo.save(profile);

        return toDto(saved);
    }

    @Override
    public BusinessProfileDto uploadLogo(Long businessId, MultipartFile logo) throws IOException {
        if (logo == null || logo.isEmpty()) {
            throw new IllegalArgumentException("Logo file is required");
        }
        String contentType = logo.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Invalid file type. Only image files are supported.");
        }
        String base64 = Base64.getEncoder().encodeToString(logo.getBytes());
        String dataUrl = "data:" + contentType + ";base64," + base64;

        BusinessProfile profile = profileRepo.findByBusinessId(businessId)
                .orElseGet(() -> BusinessProfile.builder().businessId(businessId).build());
        profile.setLogo(dataUrl);

        return toDto(profileRepo.save(profile));
    }

    private BusinessProfileDto toDto(BusinessProfile p) {
        return BusinessProfileDto.builder()
                .id(p.getId())
                .businessName(p.getBusinessName())
                .ownerName(p.getOwnerName())
                .logo(p.getLogo())
                .email(p.getEmail())
                .phone(p.getPhone())
                .address(p.getAddress())
                .currency(p.getCurrency())
                .invoicePrefix(p.getInvoicePrefix())
                .brandColor(p.getBrandColor())
                .industry(p.getIndustry())
                .country(p.getCountry())
                .brandTagline(p.getBrandTagline())
                .build();
    }
}