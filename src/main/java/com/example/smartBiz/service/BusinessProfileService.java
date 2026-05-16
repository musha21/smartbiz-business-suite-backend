package com.example.smartBiz.service;

import com.example.smartBiz.dto.BusinessProfileDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface BusinessProfileService {
    BusinessProfileDto getProfile(Long businessId);

    BusinessProfileDto createProfile(Long businessId, BusinessProfileDto dto);

    BusinessProfileDto updateProfile(Long businessId, BusinessProfileDto dto);

    BusinessProfileDto uploadLogo(Long businessId, MultipartFile logo) throws IOException;
}
