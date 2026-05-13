package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.LandingStatDto;
import com.example.smartBiz.entity.LandingStat;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.LandingStatRepo;
import com.example.smartBiz.service.LandingStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LandingStatServiceImpl implements LandingStatService {

    private final LandingStatRepo landingStatRepo;

    @Override
    @Transactional
    public LandingStatDto create(LandingStatDto dto) {
        LandingStat stat = LandingStat.builder()
                .label(dto.getLabel())
                .value(dto.getValue())
                .icon(dto.getIcon())
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();
        return mapToDto(landingStatRepo.save(stat));
    }

    @Override
    @Transactional
    public LandingStatDto update(Long id, LandingStatDto dto) {
        LandingStat stat = landingStatRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Landing stat not found: " + id));
        
        stat.setLabel(dto.getLabel());
        stat.setValue(dto.getValue());
        stat.setIcon(dto.getIcon());
        stat.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : stat.getDisplayOrder());
        stat.setActive(dto.getActive() != null ? dto.getActive() : stat.getActive());
        
        return mapToDto(landingStatRepo.save(stat));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        LandingStat stat = landingStatRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Landing stat not found: " + id));
        landingStatRepo.delete(stat);
    }

    @Override
    @Transactional
    public LandingStatDto toggleActive(Long id) {
        LandingStat stat = landingStatRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Landing stat not found: " + id));
        stat.setActive(!stat.getActive());
        return mapToDto(landingStatRepo.save(stat));
    }

    @Override
    public List<LandingStatDto> getAllForAdmin() {
        return landingStatRepo.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<LandingStatDto> getActiveForPublic() {
        return landingStatRepo.findAllByActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private LandingStatDto mapToDto(LandingStat stat) {
        return LandingStatDto.builder()
                .id(stat.getId())
                .label(stat.getLabel())
                .value(stat.getValue())
                .icon(stat.getIcon())
                .displayOrder(stat.getDisplayOrder())
                .active(stat.getActive())
                .createdAt(stat.getCreatedAt())
                .updatedAt(stat.getUpdatedAt())
                .build();
    }
}
