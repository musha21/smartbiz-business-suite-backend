package com.example.smartBiz.controller;

import com.example.smartBiz.dto.PurchaseOrderResponse;
import com.example.smartBiz.dto.ReorderAlertResponse;
import com.example.smartBiz.service.ReorderAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/api/reorder-alerts")
@Tag(name = "Reorder Alerts", description = "Stock level monitoring and automatic reorder suggestions")
public class ReorderAlertController {

    private final ReorderAlertService reorderAlertService;

    public ReorderAlertController(ReorderAlertService reorderAlertService) {
        this.reorderAlertService = reorderAlertService;
    }

    @Operation(summary = "Get active reorder alerts")
    @ApiResponse(responseCode = "200", description = "List of active alerts")
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<ReorderAlertResponse>> getActiveAlerts() {
        List<ReorderAlertResponse> alerts = reorderAlertService.getActiveAlerts();
        return ResponseEntity.ok(alerts);
    }

    @Operation(summary = "Get all reorder alerts")
    @ApiResponse(responseCode = "200", description = "List of all alerts")
    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<ReorderAlertResponse>> getAllAlerts() {
        List<ReorderAlertResponse> alerts = reorderAlertService.getAllAlerts();
        return ResponseEntity.ok(alerts);
    }

    @Operation(summary = "Get reorder alert by ID")
    @ApiResponse(responseCode = "200", description = "Alert found")
    @ApiResponse(responseCode = "404", description = "Alert not found")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ReorderAlertResponse> getAlertById(
            @Parameter(description = "Alert ID") @PathVariable Long id) {
        ReorderAlertResponse alert = reorderAlertService.getAlertById(id);
        return ResponseEntity.ok(alert);
    }

    @Operation(summary = "Dismiss a reorder alert")
    @ApiResponse(responseCode = "204", description = "Alert dismissed")
    @PutMapping("/{id}/dismiss")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Void> dismissAlert(
            @Parameter(description = "Alert ID") @PathVariable Long id) {
        reorderAlertService.dismissAlert(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Create purchase order from alert")
    @ApiResponse(responseCode = "201", description = "Purchase order created from alert")
    @ApiResponse(responseCode = "400", description = "Alert not active or no supplier linked")
    @PostMapping("/{id}/create-po")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<PurchaseOrderResponse> createPOFromAlert(
            @Parameter(description = "Alert ID") @PathVariable Long id,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Boolean allowUnlinkedSupplier) {
        PurchaseOrderResponse response = reorderAlertService.createPurchaseOrderFromAlert(id, supplierId, allowUnlinkedSupplier);
        return ResponseEntity.status(201).body(response);
    }

    @Operation(summary = "Check and generate new alerts")
    @ApiResponse(responseCode = "200", description = "Alerts checked and generated if needed")
    @PostMapping("/check")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Map<String, String>> checkAndGenerateAlerts() {
        reorderAlertService.checkAndGenerateAlerts();
        return ResponseEntity.ok(Map.of("message", "Alerts checked and generated"));
    }

    @Operation(summary = "Get count of active alerts")
    @ApiResponse(responseCode = "200", description = "Active alert count")
    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Map<String, Long>> getActiveAlertCount() {
        Long count = reorderAlertService.getActiveAlertCount();
        return ResponseEntity.ok(Map.of("count", count));
    }
}
