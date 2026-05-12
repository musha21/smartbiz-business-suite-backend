package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.TestimonialRequestDto;
import com.example.smartBiz.dto.TestimonialResponseDto;
import com.example.smartBiz.entity.Testimonial;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.TestimonialRepo;
import com.example.smartBiz.service.TestimonialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestimonialServiceImpl implements TestimonialService {

    private final TestimonialRepo testimonialRepo;

    @Override
    public TestimonialResponseDto create(TestimonialRequestDto dto) {
        Testimonial testimonial = new Testimonial();
        mapDtoToEntity(dto, testimonial);
        return mapEntityToDto(testimonialRepo.save(testimonial));
    }

    @Override
    public TestimonialResponseDto update(Long id, TestimonialRequestDto dto) {
        Testimonial testimonial = testimonialRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Testimonial not found with id: " + id));
        mapDtoToEntity(dto, testimonial);
        return mapEntityToDto(testimonialRepo.save(testimonial));
    }

    @Override
    public void delete(Long id) {
        if (!testimonialRepo.existsById(id)) {
            throw new ResourceNotFoundException("Testimonial not found with id: " + id);
        }
        testimonialRepo.deleteById(id);
    }

    @Override
    public List<TestimonialResponseDto> getAllForAdmin() {
        return testimonialRepo.findAll().stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<TestimonialResponseDto> getActiveForPublic() {
        return testimonialRepo.findAllByActiveTrueOrderByCreatedAtDesc().stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    private void mapDtoToEntity(TestimonialRequestDto dto, Testimonial entity) {
        entity.setAuthorName(dto.getAuthorName());
        entity.setAuthorRole(dto.getAuthorRole());
        entity.setAuthorCompany(dto.getAuthorCompany());
        entity.setContent(dto.getContent());
        entity.setRating(dto.getRating());
        entity.setAvatarUrl(dto.getAvatarUrl());
        if (dto.getActive() != null) {
            entity.setActive(dto.getActive());
        }
    }

    private TestimonialResponseDto mapEntityToDto(Testimonial entity) {
        return TestimonialResponseDto.builder()
                .id(entity.getId())
                .authorName(entity.getAuthorName())
                .authorRole(entity.getAuthorRole())
                .authorCompany(entity.getAuthorCompany())
                .content(entity.getContent())
                .rating(entity.getRating())
                .avatarUrl(entity.getAvatarUrl())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
