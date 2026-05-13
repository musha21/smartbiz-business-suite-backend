package com.example.smartBiz.service;

import com.example.smartBiz.dto.ReorderDto;
import com.example.smartBiz.dto.TrustIntegrationDto;

import java.util.List;

public interface TrustIntegrationService {
    TrustIntegrationDto create(TrustIntegrationDto dto);
    TrustIntegrationDto update(Long id, TrustIntegrationDto dto);
    void delete(Long id);
    TrustIntegrationDto toggleActive(Long id);
    void reorder(List<ReorderDto> reorderList);
    List<TrustIntegrationDto> getAllForAdmin();
    List<TrustIntegrationDto> getActiveForPublic();
}
