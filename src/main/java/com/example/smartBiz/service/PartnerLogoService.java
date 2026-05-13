package com.example.smartBiz.service;

import com.example.smartBiz.dto.PartnerLogoDto;

import java.util.List;

public interface PartnerLogoService {
    PartnerLogoDto create(PartnerLogoDto dto);
    PartnerLogoDto update(Long id, PartnerLogoDto dto);
    void delete(Long id);
    List<PartnerLogoDto> getAllForAdmin();
    List<PartnerLogoDto> getActiveForPublic();
}
