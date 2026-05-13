package com.example.smartBiz.controller;

import com.example.smartBiz.dto.ReorderDto;
import com.example.smartBiz.dto.TrustIntegrationDto;
import com.example.smartBiz.service.TrustIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/api/admin/integrations")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "Admin Trust Integrations", description = "Trust integration management")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTrustIntegrationController {

    private final TrustIntegrationService trustIntegrationService;

    @Operation(summary = "Get all trust integrations (admin view)")
    @GetMapping
    public ResponseEntity<List<TrustIntegrationDto>> getAll() {
        return ResponseEntity.ok(trustIntegrationService.getAllForAdmin());
    }

    @Operation(summary = "Create trust integration")
    @PostMapping
    public ResponseEntity<TrustIntegrationDto> create(@RequestBody TrustIntegrationDto dto) {
        return ResponseEntity.ok(trustIntegrationService.create(dto));
    }

    @Operation(summary = "Update trust integration")
    @PutMapping("/{id}")
    public ResponseEntity<TrustIntegrationDto> update(@PathVariable Long id, @RequestBody TrustIntegrationDto dto) {
        return ResponseEntity.ok(trustIntegrationService.update(id, dto));
    }

    @Operation(summary = "Delete trust integration")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        trustIntegrationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Toggle integration active status")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<TrustIntegrationDto> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(trustIntegrationService.toggleActive(id));
    }

    @Operation(summary = "Bulk reorder integrations")
    @PutMapping("/reorder")
    public ResponseEntity<Void> reorder(@RequestBody List<ReorderDto> reorderList) {
        trustIntegrationService.reorder(reorderList);
        return ResponseEntity.ok().build();
    }
}
