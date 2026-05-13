package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.FAQDto;
import com.example.smartBiz.entity.FAQ;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.FAQRepo;
import com.example.smartBiz.service.FAQService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FAQServiceImpl implements FAQService {

    private final FAQRepo faqRepo;

    @Override
    @Transactional
    public FAQDto create(FAQDto dto) {
        FAQ faq = FAQ.builder()
                .question(dto.getQuestion())
                .answer(dto.getAnswer())
                .category(dto.getCategory())
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();
        return mapToDto(faqRepo.save(faq));
    }

    @Override
    @Transactional
    public FAQDto update(Long id, FAQDto dto) {
        FAQ faq = faqRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FAQ not found: " + id));
        
        faq.setQuestion(dto.getQuestion());
        faq.setAnswer(dto.getAnswer());
        faq.setCategory(dto.getCategory());
        faq.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : faq.getDisplayOrder());
        faq.setActive(dto.getActive() != null ? dto.getActive() : faq.getActive());
        
        return mapToDto(faqRepo.save(faq));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        FAQ faq = faqRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FAQ not found: " + id));
        faqRepo.delete(faq);
    }

    @Override
    @Transactional
    public FAQDto toggleActive(Long id) {
        FAQ faq = faqRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FAQ not found: " + id));
        faq.setActive(!faq.getActive());
        return mapToDto(faqRepo.save(faq));
    }

    @Override
    public List<FAQDto> getAllForAdmin() {
        return faqRepo.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<FAQDto> getActiveForPublic() {
        return faqRepo.findAllByActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private FAQDto mapToDto(FAQ faq) {
        return FAQDto.builder()
                .id(faq.getId())
                .question(faq.getQuestion())
                .answer(faq.getAnswer())
                .category(faq.getCategory())
                .displayOrder(faq.getDisplayOrder())
                .active(faq.getActive())
                .createdAt(faq.getCreatedAt())
                .updatedAt(faq.getUpdatedAt())
                .build();
    }
}
