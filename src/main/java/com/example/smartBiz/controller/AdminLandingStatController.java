package com.example.smartBiz.controller;

import com.example.smartBiz.dto.LandingStatDto;
import com.example.smartBiz.service.LandingStatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/api/admin/")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "Admin Landing Stats", description = "Landing page stats management")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLandingStatController {

    private final LandingStatService landingStatService;

    @Operation(summary = "Get all landing stats (admin view)")
    @GetMapping("/states")
    public ResponseEntity<List<LandingStatDto>> getAll() {
        return ResponseEntity.ok(landingStatService.getAllForAdmin());
    }

    @Operation(summary = "Create landing stat")
    @PostMapping
    public ResponseEntity<LandingStatDto> create(@RequestBody LandingStatDto dto) {
        return ResponseEntity.ok(landingStatService.create(dto));
    }

    @Operation(summary = "Update landing stat")
    @PutMapping("/{id}")
    public ResponseEntity<LandingStatDto> update(@PathVariable Long id, @RequestBody LandingStatDto dto) {
        return ResponseEntity.ok(landingStatService.update(id, dto));
    }

    @Operation(summary = "Delete landing stat")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        landingStatService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Toggle stat active status")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<LandingStatDto> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(landingStatService.toggleActive(id));
    }
}
