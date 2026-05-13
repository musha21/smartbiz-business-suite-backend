package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.PartnerLogoDto;
import com.example.smartBiz.entity.PartnerLogo;
import com.example.smartBiz.repository.PartnerLogoRepo;
import com.example.smartBiz.service.PartnerLogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PartnerLogoServiceImpl implements PartnerLogoService {

    private final PartnerLogoRepo partnerLogoRepo;

    @Override
    public PartnerLogoDto create(PartnerLogoDto dto) {
        PartnerLogo partnerLogo = new PartnerLogo();
        partnerLogo.setCompanyName(dto.getCompanyName());
        validateLogo(dto.getLogo());
        partnerLogo.setLogo(dto.getLogo());
        partnerLogo.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);
        partnerLogo.setActive(dto.getActive() != null ? dto.getActive() : true);
        
        PartnerLogo saved = partnerLogoRepo.save(partnerLogo);
        return mapToDto(saved);
    }

    @Override
    public PartnerLogoDto update(Long id, PartnerLogoDto dto) {
        PartnerLogo partnerLogo = partnerLogoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Partner logo not found with id: " + id));
        
        partnerLogo.setCompanyName(dto.getCompanyName());
        if (dto.getLogo() != null) {
            validateLogo(dto.getLogo());
            partnerLogo.setLogo(dto.getLogo());
        }
        partnerLogo.setDisplayOrder(dto.getDisplayOrder());
        partnerLogo.setActive(dto.getActive());
        
        PartnerLogo updated = partnerLogoRepo.save(partnerLogo);
        return mapToDto(updated);
    }

    @Override
    public void delete(Long id) {
        partnerLogoRepo.deleteById(id);
    }

    @Override
    public List<PartnerLogoDto> getAllForAdmin() {
        return partnerLogoRepo.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<PartnerLogoDto> getActiveForPublic() {
        return partnerLogoRepo.findAllByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private PartnerLogoDto mapToDto(PartnerLogo partnerLogo) {
        return PartnerLogoDto.builder()
                .id(partnerLogo.getId())
                .companyName(partnerLogo.getCompanyName())
                .logo(partnerLogo.getLogo())
                .displayOrder(partnerLogo.getDisplayOrder())
                .active(partnerLogo.getActive())
                .createdAt(partnerLogo.getCreatedAt())
                .updatedAt(partnerLogo.getUpdatedAt())
                .build();
    }

    private void validateLogo(String logoData) {
        if (logoData == null || logoData.isBlank()) {
            throw new RuntimeException("Logo data is required");
        }
        
        // Support common image prefixes
        boolean isValidPrefix = logoData.startsWith("data:image/png;base64,") || 
                               logoData.startsWith("data:image/jpeg;base64,") || 
                               logoData.startsWith("data:image/jpg;base64,") ||
                               logoData.startsWith("data:image/webp;base64,") ||
                               logoData.startsWith("data:image/svg+xml;base64,");
                               
        if (!isValidPrefix) {
            throw new RuntimeException("Invalid image format. Supported: PNG, JPG, JPEG, WEBP, SVG");
        }
    }
}
