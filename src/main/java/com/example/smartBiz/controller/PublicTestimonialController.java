package com.example.smartBiz.controller;

import com.example.smartBiz.dto.TestimonialResponseDto;
import com.example.smartBiz.service.TestimonialService;
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
@RequestMapping("/v1/api/public/testimonials")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "Public Testimonials", description = "Publicly visible testimonials for landing page")
public class PublicTestimonialController {

    private final TestimonialService testimonialService;

    @Operation(summary = "List active testimonials")
    @GetMapping
    public ResponseEntity<List<TestimonialResponseDto>> getActive() {
        return ResponseEntity.ok(testimonialService.getActiveForPublic());
    }
}
