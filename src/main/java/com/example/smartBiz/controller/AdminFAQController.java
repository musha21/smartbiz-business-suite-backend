package com.example.smartBiz.controller;

import com.example.smartBiz.dto.FAQDto;
import com.example.smartBiz.service.FAQService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/api/admin/faqs")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "Admin FAQs", description = "FAQ management for landing page")
@PreAuthorize("hasRole('ADMIN')")
public class AdminFAQController {

    private final FAQService faqService;

    @Operation(summary = "Get all FAQs (admin view)")
    @GetMapping
    public ResponseEntity<List<FAQDto>> getAll() {
        return ResponseEntity.ok(faqService.getAllForAdmin());
    }

    @Operation(summary = "Create FAQ")
    @PostMapping
    public ResponseEntity<FAQDto> create(@RequestBody FAQDto dto) {
        return ResponseEntity.ok(faqService.create(dto));
    }

    @Operation(summary = "Update FAQ")
    @PutMapping("/{id}")
    public ResponseEntity<FAQDto> update(@PathVariable Long id, @RequestBody FAQDto dto) {
        return ResponseEntity.ok(faqService.update(id, dto));
    }

    @Operation(summary = "Delete FAQ")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        faqService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Toggle FAQ active status")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<FAQDto> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(faqService.toggleActive(id));
    }
}
