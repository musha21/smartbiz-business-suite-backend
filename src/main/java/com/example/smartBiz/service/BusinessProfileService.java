package com.example.smartBiz.service;

import com.example.smartBiz.dto.BusinessProfileDto;

public interface BusinessProfileService {
    BusinessProfileDto getProfile(Long businessId);

    BusinessProfileDto createProfile(Long businessId, BusinessProfileDto dto);

    BusinessProfileDto updateProfile(Long businessId, BusinessProfileDto dto);
}
