package com.example.smartBiz.controller;

import com.example.smartBiz.dto.PurchaseOrderCreateRequest;
import com.example.smartBiz.dto.PurchaseOrderResponse;
import com.example.smartBiz.dto.ReceiveGoodsRequest;
import com.example.smartBiz.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/api/purchase-orders")
@Tag(name = "Purchase Orders", description = "Purchase order management - create, send, receive, and track POs")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @Operation(summary = "Create a new purchase order")
    @ApiResponse(responseCode = "201", description = "Purchase order created")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<PurchaseOrderResponse> createPurchaseOrder(
            @Valid @RequestBody PurchaseOrderCreateRequest request) {
        PurchaseOrderResponse response = purchaseOrderService.createPurchaseOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Send purchase order to supplier")
    @ApiResponse(responseCode = "200", description = "Purchase order sent")
    @ApiResponse(responseCode = "404", description = "Purchase order not found")
    @PutMapping("/{id}/send")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<PurchaseOrderResponse> sendPurchaseOrder(
            @Parameter(description = "Purchase Order ID") @PathVariable Long id) {
        PurchaseOrderResponse response = purchaseOrderService.sendPurchaseOrder(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Receive goods for a purchase order")
    @ApiResponse(responseCode = "200", description = "Goods received, inventory updated")
    @ApiResponse(responseCode = "400", description = "Invalid receipt data")
    @PutMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<PurchaseOrderResponse> receiveGoods(
            @Parameter(description = "Purchase Order ID") @PathVariable Long id,
            @Valid @RequestBody ReceiveGoodsRequest request) {
        PurchaseOrderResponse response = purchaseOrderService.receiveGoods(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cancel a purchase order")
    @ApiResponse(responseCode = "200", description = "Purchase order cancelled")
    @ApiResponse(responseCode = "400", description = "Cannot cancel received PO")
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<PurchaseOrderResponse> cancelPurchaseOrder(
            @Parameter(description = "Purchase Order ID") @PathVariable Long id) {
        PurchaseOrderResponse response = purchaseOrderService.cancelPurchaseOrder(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get purchase order by ID")
    @ApiResponse(responseCode = "200", description = "Purchase order found")
    @ApiResponse(responseCode = "404", description = "Purchase order not found")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<PurchaseOrderResponse> getPurchaseOrderById(
            @Parameter(description = "Purchase Order ID") @PathVariable Long id) {
        PurchaseOrderResponse response = purchaseOrderService.getPurchaseOrderById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List all purchase orders")
    @ApiResponse(responseCode = "200", description = "List of purchase orders")
    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Page<PurchaseOrderResponse>> getAllPurchaseOrders(Pageable pageable) {
        Page<PurchaseOrderResponse> response = purchaseOrderService.getAllPurchaseOrders(pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List purchase orders by status")
    @ApiResponse(responseCode = "200", description = "Filtered list of purchase orders")
    @GetMapping("/by-status/{status}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<PurchaseOrderResponse>> getPurchaseOrdersByStatus(
            @Parameter(description = "Status: DRAFT, SENT, PARTIAL, RECEIVED, CANCELLED")
            @PathVariable String status) {
        List<PurchaseOrderResponse> response = purchaseOrderService.getPurchaseOrdersByStatus(status);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get overdue purchase orders")
    @ApiResponse(responseCode = "200", description = "List of overdue purchase orders")
    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<PurchaseOrderResponse>> getOverduePurchaseOrders() {
        List<PurchaseOrderResponse> response = purchaseOrderService.getOverduePurchaseOrders();
        return ResponseEntity.ok(response);
    }
}
