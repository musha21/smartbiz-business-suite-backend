package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.*;
import com.example.smartBiz.entity.*;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.exception.ValidationException;
import com.example.smartBiz.repository.*;
import com.example.smartBiz.security.CustomUserPrincipal;
import com.example.smartBiz.service.PurchaseOrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepo purchaseOrderRepo;
    private final POLineItemRepo poLineItemRepo;
    private final SupplierRepo supplierRepo;
    private final ProductRepo productRepo;
    private final ProductBatchRepo productBatchRepo;
    private final ProductSupplierRepo productSupplierRepo;
    private final ReorderAlertRepo reorderAlertRepo;

    public PurchaseOrderServiceImpl(PurchaseOrderRepo purchaseOrderRepo,
                                    POLineItemRepo poLineItemRepo,
                                    SupplierRepo supplierRepo,
                                    ProductRepo productRepo,
                                    ProductBatchRepo productBatchRepo,
                                    ProductSupplierRepo productSupplierRepo,
                                    ReorderAlertRepo reorderAlertRepo) {
        this.purchaseOrderRepo = purchaseOrderRepo;
        this.poLineItemRepo = poLineItemRepo;
        this.supplierRepo = supplierRepo;
        this.productRepo = productRepo;
        this.productBatchRepo = productBatchRepo;
        this.productSupplierRepo = productSupplierRepo;
        this.reorderAlertRepo = reorderAlertRepo;
    }

    private Long requireBusinessId() {
        CustomUserPrincipal principal = CustomUserPrincipal.getCurrent();
        if (principal == null || principal.getBusinessId() == null) {
            throw new ResourceNotFoundException("Business context missing (JWT required)");
        }
        return principal.getBusinessId();
    }

    @Override
    public PurchaseOrderResponse createPurchaseOrder(PurchaseOrderCreateRequest request) {
        Long businessId = requireBusinessId();

        Supplier supplier = supplierRepo.findByIdAndBusinessId(request.getSupplierId(), businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));

        if (supplier.getArchived()) {
            throw new ValidationException("Cannot create PO with archived supplier");
        }

        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber(generatePONumber())
                .businessId(businessId)
                .supplier(supplier)
                .status(PurchaseOrder.POStatus.DRAFT)
                .orderDate(request.getOrderDate() != null ? request.getOrderDate() : LocalDate.now())
                .expectedDelivery(request.getExpectedDelivery())
                .notes(request.getNotes())
                .build();

        for (POLineItemRequestDto itemDto : request.getItems()) {
            Products product = productRepo.findById(itemDto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemDto.getProductId()));

            if (!product.getBusinessId().equals(businessId)) {
                throw new ResourceNotFoundException("Product not in your business");
            }

            if (!Boolean.TRUE.equals(request.getAllowUnlinkedSupplier())) {
                boolean linked = productSupplierRepo.existsByProduct_IdAndSupplier_Id(product.getId(), supplier.getId());
                if (!linked) {
                    throw new ValidationException(
                        "Supplier not linked to product: " + product.getName() + ". Use allowUnlinkedSupplier=true to override.");
                }
            }

            BigDecimal unitCost = itemDto.getUnitCost();
            if (unitCost == null) {
                ProductSupplier link = productSupplierRepo
                        .findByProduct_IdAndSupplier_Id(product.getId(), supplier.getId())
                        .orElse(null);
                unitCost = link != null ? link.getUnitCost() : BigDecimal.ZERO;
            }

            POLineItem lineItem = POLineItem.builder()
                    .product(product)
                    .quantityOrdered(itemDto.getQuantity())
                    .quantityReceived(0)
                    .unitCost(unitCost)
                    .build();
            lineItem.calculateLineTotal();

            po.addLineItem(lineItem);
        }

        po.calculateTotals();
        PurchaseOrder saved = purchaseOrderRepo.save(po);

        return mapToResponse(saved);
    }

    @Override
    public PurchaseOrderResponse sendPurchaseOrder(Long poId) {
        Long businessId = requireBusinessId();

        PurchaseOrder po = purchaseOrderRepo.findByIdAndBusinessId(poId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found"));

        if (po.getStatus() != PurchaseOrder.POStatus.DRAFT) {
            throw new ValidationException("Only DRAFT POs can be sent");
        }

        po.setStatus(PurchaseOrder.POStatus.SENT);
        if (po.getOrderDate() == null) {
            po.setOrderDate(LocalDate.now());
        }

        PurchaseOrder saved = purchaseOrderRepo.save(po);
        return mapToResponse(saved);
    }

    @Override
    public PurchaseOrderResponse receiveGoods(Long poId, ReceiveGoodsRequest request) {
        Long businessId = requireBusinessId();

        PurchaseOrder po = purchaseOrderRepo.findByIdWithLineItems(poId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found"));

        if (po.getStatus() != PurchaseOrder.POStatus.SENT && po.getStatus() != PurchaseOrder.POStatus.PARTIAL) {
            throw new ValidationException("Can only receive goods for SENT or PARTIAL POs");
        }

        boolean allFullyReceived = true;
        boolean anyReceived = false;

        for (ReceiveGoodsRequest.ReceivedItemDto received : request.getReceivedItems()) {
            POLineItem lineItem = po.getLineItems().stream()
                    .filter(li -> li.getId().equals(received.getLineItemId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Line item not found: " + received.getLineItemId()));

            int newReceived = lineItem.getQuantityReceived() + received.getQuantityReceived();
            lineItem.setQuantityReceived(newReceived);

            if (received.getQuantityReceived() > 0) {
                anyReceived = true;

                String batchNumber = received.getBatchNumber() != null ? received.getBatchNumber()
                        : generateBatchNumber(po.getPoNumber(), lineItem.getProduct().getId());

                ProductBatch batch = ProductBatch.builder()
                        .businessId(businessId)
                        .product(lineItem.getProduct())
                        .batchNumber(batchNumber)
                        .qtyAvailable(received.getQuantityReceived())
                        .createdAt(LocalDateTime.now())
                        .build();

                ProductBatch savedBatch = productBatchRepo.save(batch);
                lineItem.setBatch(savedBatch);
            }

            if (!lineItem.isFullyReceived()) {
                allFullyReceived = false;
            }
        }

        if (!anyReceived) {
            throw new ValidationException("At least one item must be received");
        }

        po.setActualDelivery(request.getActualDeliveryDate());

        if (allFullyReceived) {
            po.setStatus(PurchaseOrder.POStatus.RECEIVED);
        } else {
            po.setStatus(PurchaseOrder.POStatus.PARTIAL);
        }

        updateSupplierReliability(po);

        PurchaseOrder saved = purchaseOrderRepo.save(po);
        return mapToResponse(saved);
    }

    private void updateSupplierReliability(PurchaseOrder po) {
        if (po.getActualDelivery() == null || po.getExpectedDelivery() == null) {
            return;
        }

        Supplier supplier = po.getSupplier();
        
        // Initialize reliability score if null (default: 0.8 for new suppliers)
        BigDecimal currentScore = supplier.getReliabilityScore();
        if (currentScore == null) {
            currentScore = BigDecimal.valueOf(0.8);
            supplier.setReliabilityScore(currentScore);
        }
        
        boolean onTime = !po.getActualDelivery().isAfter(po.getExpectedDelivery());

        if (!onTime) {
            supplier.setLateDeliveryCount(supplier.getLateDeliveryCount() + 1);

            if (supplier.getLateDeliveryCount() >= 2) {
                BigDecimal newScore = currentScore
                        .multiply(BigDecimal.valueOf(0.9))
                        .max(BigDecimal.valueOf(0.5));
                supplier.setReliabilityScore(newScore);
            }
        } else {
            BigDecimal newScore = currentScore
                    .multiply(BigDecimal.valueOf(0.95))
                    .add(BigDecimal.valueOf(0.05))
                    .min(BigDecimal.ONE);
            supplier.setReliabilityScore(newScore);
        }

        supplierRepo.save(supplier);
    }

    @Override
    public PurchaseOrderResponse cancelPurchaseOrder(Long poId) {
        Long businessId = requireBusinessId();

        PurchaseOrder po = purchaseOrderRepo.findByIdAndBusinessId(poId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found"));

        if (po.getStatus() == PurchaseOrder.POStatus.RECEIVED) {
            throw new ValidationException("Cannot cancel already received PO");
        }

        po.setStatus(PurchaseOrder.POStatus.CANCELLED);
        PurchaseOrder saved = purchaseOrderRepo.save(po);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderResponse getPurchaseOrderById(Long id) {
        Long businessId = requireBusinessId();

        PurchaseOrder po = purchaseOrderRepo.findByIdWithLineItems(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found"));

        return mapToResponse(po);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseOrderResponse> getAllPurchaseOrders(Pageable pageable) {
        Long businessId = requireBusinessId();

        return purchaseOrderRepo.findByBusinessIdWithSupplier(businessId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> getPurchaseOrdersByStatus(String status) {
        Long businessId = requireBusinessId();

        try {
            PurchaseOrder.POStatus poStatus = PurchaseOrder.POStatus.valueOf(status.toUpperCase());
            return purchaseOrderRepo.findByBusinessIdAndStatusWithSupplier(businessId, poStatus)
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid status: " + status);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> getOverduePurchaseOrders() {
        Long businessId = requireBusinessId();

        return purchaseOrderRepo.findOverdueOrders(businessId, LocalDate.now())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public synchronized String generatePONumber() {
        String year = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
        
        // Get max sequence from database for current year (handles server restarts)
        int maxSeq = purchaseOrderRepo.findMaxSequenceForYear(year).orElse(0);
        int nextSeq = maxSeq + 1;
        
        return String.format("PO-%s-%04d", year, nextSeq);
    }

    private String generateBatchNumber(String poNumber, Long productId) {
        return String.format("%s-P%d", poNumber, productId);
    }

    private PurchaseOrderResponse mapToResponse(PurchaseOrder po) {
        return PurchaseOrderResponse.builder()
                .id(po.getId())
                .poNumber(po.getPoNumber())
                .supplierId(po.getSupplier().getId())
                .supplierName(po.getSupplier().getName())
                .status(po.getStatus().name())
                .orderDate(po.getOrderDate())
                .expectedDelivery(po.getExpectedDelivery())
                .actualDelivery(po.getActualDelivery())
                .subtotal(po.getSubtotal())
                .tax(po.getTax())
                .shipping(po.getShipping())
                .total(po.getTotal())
                .notes(po.getNotes())
                .lineItems(po.getLineItems().stream()
                        .map(this::mapLineItemToResponse)
                        .toList())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private POLineItemResponseDto mapLineItemToResponse(POLineItem lineItem) {
        return POLineItemResponseDto.builder()
                .id(lineItem.getId())
                .productId(lineItem.getProduct().getId())
                .productName(lineItem.getProduct().getName())
                .productSku(lineItem.getProduct().getSku())
                .batchId(lineItem.getBatch() != null ? lineItem.getBatch().getId() : null)
                .batchNumber(lineItem.getBatch() != null ? lineItem.getBatch().getBatchNumber() : null)
                .quantityOrdered(lineItem.getQuantityOrdered())
                .quantityReceived(lineItem.getQuantityReceived())
                .unitCost(lineItem.getUnitCost())
                .lineTotal(lineItem.getLineTotal())
                .fullyReceived(lineItem.isFullyReceived())
                .build();
    }
}
