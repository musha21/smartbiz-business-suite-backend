package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.ReorderDto;
import com.example.smartBiz.dto.TrustIntegrationDto;
import com.example.smartBiz.entity.TrustIntegration;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.TrustIntegrationRepo;
import com.example.smartBiz.service.TrustIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrustIntegrationServiceImpl implements TrustIntegrationService {

    private final TrustIntegrationRepo trustIntegrationRepo;

    @Override
    @Transactional
    public TrustIntegrationDto create(TrustIntegrationDto dto) {
        TrustIntegration integration = TrustIntegration.builder()
                .label(dto.getLabel())
                .icon(dto.getIcon())
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();
        return mapToDto(trustIntegrationRepo.save(integration));
    }

    @Override
    @Transactional
    public TrustIntegrationDto update(Long id, TrustIntegrationDto dto) {
        TrustIntegration integration = trustIntegrationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trust integration not found: " + id));
        
        integration.setLabel(dto.getLabel());
        integration.setIcon(dto.getIcon());
        integration.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : integration.getDisplayOrder());
        integration.setActive(dto.getActive() != null ? dto.getActive() : integration.getActive());
        
        return mapToDto(trustIntegrationRepo.save(integration));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TrustIntegration integration = trustIntegrationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trust integration not found: " + id));
        trustIntegrationRepo.delete(integration);
    }

    @Override
    @Transactional
    public TrustIntegrationDto toggleActive(Long id) {
        TrustIntegration integration = trustIntegrationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trust integration not found: " + id));
        integration.setActive(!integration.getActive());
        return mapToDto(trustIntegrationRepo.save(integration));
    }

    @Override
    @Transactional
    public void reorder(List<ReorderDto> reorderList) {
        for (ReorderDto dto : reorderList) {
            TrustIntegration integration = trustIntegrationRepo.findById(dto.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Trust integration not found: " + dto.getId()));
            integration.setDisplayOrder(dto.getDisplayOrder());
            trustIntegrationRepo.save(integration);
        }
    }

    @Override
    public List<TrustIntegrationDto> getAllForAdmin() {
        return trustIntegrationRepo.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<TrustIntegrationDto> getActiveForPublic() {
        return trustIntegrationRepo.findAllByActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private TrustIntegrationDto mapToDto(TrustIntegration integration) {
        return TrustIntegrationDto.builder()
                .id(integration.getId())
                .label(integration.getLabel())
                .icon(integration.getIcon())
                .displayOrder(integration.getDisplayOrder())
                .active(integration.getActive())
                .createdAt(integration.getCreatedAt())
                .updatedAt(integration.getUpdatedAt())
                .build();
    }
}
