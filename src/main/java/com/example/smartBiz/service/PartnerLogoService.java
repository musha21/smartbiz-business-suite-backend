package com.example.smartBiz.service;

import com.example.smartBiz.dto.PartnerLogoDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface PartnerLogoService {
    PartnerLogoDto create(String companyName, Integer displayOrder, Boolean active, MultipartFile logo) throws IOException;
    PartnerLogoDto update(Long id, String companyName, Integer displayOrder, Boolean active, MultipartFile logo) throws IOException;
    void delete(Long id);
    List<PartnerLogoDto> getAllForAdmin();
    List<PartnerLogoDto> getActiveForPublic();
    PartnerLogoDto toggleActive(Long id);
}
