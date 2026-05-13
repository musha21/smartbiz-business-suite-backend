package com.example.smartBiz.controller;

import com.example.smartBiz.dto.FAQDto;
import com.example.smartBiz.service.FAQService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/api/public/faqs")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "Public FAQs", description = "Public FAQ fetch for landing page")
public class PublicFAQController {

    private final FAQService faqService;

    @Operation(summary = "Get all active FAQs")
    @GetMapping
    public ResponseEntity<List<FAQDto>> getActiveFAQs() {
        return ResponseEntity.ok(faqService.getActiveForPublic());
    }
}
