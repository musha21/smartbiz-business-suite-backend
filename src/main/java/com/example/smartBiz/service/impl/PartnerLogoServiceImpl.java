package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.PartnerLogoDto;
import com.example.smartBiz.entity.PartnerLogo;
import com.example.smartBiz.repository.PartnerLogoRepo;
import com.example.smartBiz.service.PartnerLogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PartnerLogoServiceImpl implements PartnerLogoService {

    private final PartnerLogoRepo partnerLogoRepo;

    @Override
    public PartnerLogoDto create(String companyName, Integer displayOrder, Boolean active, MultipartFile logo) throws IOException {
        PartnerLogo partnerLogo = new PartnerLogo();
        partnerLogo.setCompanyName(companyName);
        partnerLogo.setLogo(toBase64DataUrl(logo));
        partnerLogo.setDisplayOrder(displayOrder != null ? displayOrder : 0);
        partnerLogo.setActive(active != null ? active : true);

        return mapToDto(partnerLogoRepo.save(partnerLogo));
    }

    @Override
    public PartnerLogoDto update(Long id, String companyName, Integer displayOrder, Boolean active, MultipartFile logo) throws IOException {
        PartnerLogo partnerLogo = partnerLogoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Partner logo not found with id: " + id));

        partnerLogo.setCompanyName(companyName);
        partnerLogo.setDisplayOrder(displayOrder != null ? displayOrder : partnerLogo.getDisplayOrder());
        partnerLogo.setActive(active != null ? active : partnerLogo.getActive());
        if (logo != null && !logo.isEmpty()) {
            partnerLogo.setLogo(toBase64DataUrl(logo));
        }

        return mapToDto(partnerLogoRepo.save(partnerLogo));
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

    @Override
    public PartnerLogoDto toggleActive(Long id) {
        PartnerLogo partnerLogo = partnerLogoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Partner logo not found with id: " + id));
        partnerLogo.setActive(!partnerLogo.getActive());
        return mapToDto(partnerLogoRepo.save(partnerLogo));
    }

    private String toBase64DataUrl(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Logo file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Invalid file type. Only image files are supported.");
        }
        String base64 = Base64.getEncoder().encodeToString(file.getBytes());
        return "data:" + contentType + ";base64," + base64;
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
}
