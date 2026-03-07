package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.BusinessProfileDto;
import com.example.smartBiz.entity.BusinessProfile;
import com.example.smartBiz.repository.BusinessProfileRepo;
import com.example.smartBiz.repository.BusinessRepo;
import com.example.smartBiz.service.BusinessProfileService;
import org.springframework.stereotype.Service;

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

        // Prevent duplicate profiles
        if (profileRepo.findByBusinessId(businessId).isPresent()) {
            throw new IllegalArgumentException(
                    "Profile already exists for this business. Use PUT to update.");
        }

        BusinessProfile profile = BusinessProfile.builder()
                .businessId(businessId)
                .businessName(dto.getBusinessName())
                .ownerName(dto.getOwnerName())
                .logo(dto.getLogo())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .currency(dto.getCurrency())
                .invoicePrefix(dto.getInvoicePrefix())
                .brandColor(dto.getBrandColor())
                .industry(dto.getIndustry())
                .country(dto.getCountry())
                .brandTagline(dto.getBrandTagline())
                .build();

        BusinessProfile saved = profileRepo.save(profile);

        return toDto(saved);
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

    private BusinessProfileDto toDto(BusinessProfile p) {
        return BusinessProfileDto.builder()
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