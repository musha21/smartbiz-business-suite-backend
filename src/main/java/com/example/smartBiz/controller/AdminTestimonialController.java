package com.example.smartBiz.controller;

import com.example.smartBiz.dto.TestimonialRequestDto;
import com.example.smartBiz.dto.TestimonialResponseDto;
import com.example.smartBiz.service.TestimonialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/api/admin/testimonials")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "Admin Testimonials", description = "Testimonial management for admins")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTestimonialController {

    private final TestimonialService testimonialService;

    @Operation(summary = "Create testimonial")
    @PostMapping
    public ResponseEntity<TestimonialResponseDto> create(@RequestBody TestimonialRequestDto dto) {
        return ResponseEntity.ok(testimonialService.create(dto));
    }

    @Operation(summary = "Update testimonial")
    @PutMapping("/{id}")
    public ResponseEntity<TestimonialResponseDto> update(@PathVariable Long id, @RequestBody TestimonialRequestDto dto) {
        return ResponseEntity.ok(testimonialService.update(id, dto));
    }

    @Operation(summary = "Delete testimonial")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        testimonialService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all testimonials")
    @GetMapping
    public ResponseEntity<List<TestimonialResponseDto>> getAll() {
        return ResponseEntity.ok(testimonialService.getAllForAdmin());
    }
}
