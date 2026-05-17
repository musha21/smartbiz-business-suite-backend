package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.POLineItemRequestDto;
import com.example.smartBiz.dto.PurchaseOrderCreateRequest;
import com.example.smartBiz.dto.PurchaseOrderResponse;
import com.example.smartBiz.dto.ReorderAlertResponse;
import com.example.smartBiz.entity.*;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.exception.ValidationException;
import com.example.smartBiz.repository.*;
import com.example.smartBiz.security.CustomUserPrincipal;
import com.example.smartBiz.service.PurchaseOrderService;
import com.example.smartBiz.service.ReorderAlertService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReorderAlertServiceImpl implements ReorderAlertService {

    private final ReorderAlertRepo reorderAlertRepo;
    private final ProductRepo productRepo;
    private final ProductBatchRepo productBatchRepo;
    private final SupplierRepo supplierRepo;
    private final ProductSupplierRepo productSupplierRepo;
    private final PurchaseOrderService purchaseOrderService;
    private final InvoiceRepo invoiceRepo;
    private final InvoiceItemRepo invoiceItemRepo;

    public ReorderAlertServiceImpl(ReorderAlertRepo reorderAlertRepo,
                                   ProductRepo productRepo,
                                   ProductBatchRepo productBatchRepo,
                                   SupplierRepo supplierRepo,
                                   ProductSupplierRepo productSupplierRepo,
                                   PurchaseOrderService purchaseOrderService,
                                   InvoiceRepo invoiceRepo,
                                   InvoiceItemRepo invoiceItemRepo) {
        this.reorderAlertRepo = reorderAlertRepo;
        this.productRepo = productRepo;
        this.productBatchRepo = productBatchRepo;
        this.supplierRepo = supplierRepo;
        this.productSupplierRepo = productSupplierRepo;
        this.purchaseOrderService = purchaseOrderService;
        this.invoiceRepo = invoiceRepo;
        this.invoiceItemRepo = invoiceItemRepo;
    }

    private Long requireBusinessId() {
        CustomUserPrincipal principal = CustomUserPrincipal.getCurrent();
        if (principal == null || principal.getBusinessId() == null) {
            throw new ResourceNotFoundException("Business context missing (JWT required)");
        }
        return principal.getBusinessId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReorderAlertResponse> getActiveAlerts() {
        Long businessId = requireBusinessId();

        return reorderAlertRepo.findByBusinessIdAndStatusOrderByTriggeredAtDesc(businessId, ReorderAlert.AlertStatus.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReorderAlertResponse> getAllAlerts() {
        Long businessId = requireBusinessId();

        return reorderAlertRepo.findByBusinessIdOrderByTriggeredAtDesc(businessId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReorderAlertResponse getAlertById(Long id) {
        Long businessId = requireBusinessId();

        ReorderAlert alert = reorderAlertRepo.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found"));

        return mapToResponse(alert);
    }

    @Override
    public void dismissAlert(Long id) {
        Long businessId = requireBusinessId();

        ReorderAlert alert = reorderAlertRepo.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found"));

        alert.markDismissed();
        reorderAlertRepo.save(alert);
    }

    @Override
    public PurchaseOrderResponse createPurchaseOrderFromAlert(Long alertId, Long supplierId, Boolean allowUnlinkedSupplier) {
        Long businessId = requireBusinessId();

        ReorderAlert alert = reorderAlertRepo.findByIdAndBusinessId(alertId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found"));

        if (alert.getStatus() != ReorderAlert.AlertStatus.ACTIVE) {
            throw new ValidationException("Alert is not active");
        }

        Supplier supplier;
        if (supplierId != null) {
            supplier = supplierRepo.findByIdAndBusinessId(supplierId, businessId)
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
        } else {
            supplier = alert.getSuggestedSupplier();
            if (supplier == null) {
                List<ProductSupplier> links = productSupplierRepo.findByProduct_IdOrderByPriorityRankAsc(alert.getProduct().getId());
                if (links.isEmpty()) {
                    throw new ValidationException("No supplier linked to this product");
                }
                supplier = links.get(0).getSupplier();
            }
        }

        int moq = supplier.getMoq() != null ? supplier.getMoq() : 1;
        int leadTime = supplier.getLeadTimeDays() != null ? supplier.getLeadTimeDays() : 7;

        int suggestedQty = Math.max(alert.getSuggestedQty(), moq);
        suggestedQty = (int) Math.ceil((double) suggestedQty / moq) * moq;

        PurchaseOrderCreateRequest request = PurchaseOrderCreateRequest.builder()
                .supplierId(supplier.getId())
                .expectedDelivery(LocalDateTime.now().plusDays(leadTime).toLocalDate())
                .items(List.of(POLineItemRequestDto.builder()
                        .productId(alert.getProduct().getId())
                        .quantity(suggestedQty)
                        .build()))
                .allowUnlinkedSupplier(allowUnlinkedSupplier != null ? allowUnlinkedSupplier : false)
                .build();

        PurchaseOrderResponse response = purchaseOrderService.createPurchaseOrder(request);

        alert.markOrdered();
        reorderAlertRepo.save(alert);

        return response;
    }

    @Override
    public void checkAndGenerateAlerts() {
        Long businessId = requireBusinessId();

        List<Products> products = productRepo.findAllByBusinessId(businessId);

        for (Products product : products) {
            Integer totalStock = productBatchRepo.sumQtyByBusinessIdAndProductId(businessId, product.getId());
            if (totalStock == null) totalStock = 0;

            BigDecimal dailyUsage = calculateDailyUsage(product.getId(), businessId);
            int leadTime = getLeadTimeForProduct(product.getId());

            Integer lowStockLimit = product.getLow_stock_limit();
            if (lowStockLimit == null) lowStockLimit = 0;

            int reorderPoint = Math.max(
                    dailyUsage.multiply(BigDecimal.valueOf(leadTime * 1.5)).setScale(0, RoundingMode.CEILING).intValue(),
                    lowStockLimit
            );

            boolean alreadyAlerted = reorderAlertRepo.existsByProduct_IdAndStatus(product.getId(), ReorderAlert.AlertStatus.ACTIVE);

            if (totalStock <= reorderPoint && !alreadyAlerted) {
                Supplier suggestedSupplier = findBestSupplier(product.getId());

                int moq = suggestedSupplier != null && suggestedSupplier.getMoq() != null
                        ? suggestedSupplier.getMoq() : 1;

                int suggestedQty = Math.max(reorderPoint - totalStock, moq);
                suggestedQty = (int) Math.ceil((double) suggestedQty / moq) * moq;

                ReorderAlert alert = ReorderAlert.builder()
                        .businessId(businessId)
                        .product(product)
                        .currentStock(totalStock)
                        .reorderPoint(reorderPoint)
                        .suggestedQty(suggestedQty)
                        .suggestedSupplier(suggestedSupplier)
                        .dailyUsageRate(dailyUsage)
                        .build();

                reorderAlertRepo.save(alert);
            }
        }
    }

    private BigDecimal calculateDailyUsage(Long productId, Long businessId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        List<InvoiceItem> items = invoiceItemRepo.findRecentSalesByProduct(productId, businessId, thirtyDaysAgo);

        if (items == null || items.isEmpty()) {
            return BigDecimal.ONE;
        }

        int totalSold = items.stream()
                .mapToInt(InvoiceItem::getQuantity)
                .sum();

        return BigDecimal.valueOf(totalSold / 30.0);
    }

    private int getLeadTimeForProduct(Long productId) {
        Optional<ProductSupplier> primary = productSupplierRepo.findPrimarySupplierForProduct(productId);

        if (primary.isPresent()) {
            Integer leadTime = primary.get().getSupplier().getLeadTimeDays();
            if (leadTime != null) return leadTime;
        }

        return 7;
    }

    private Supplier findBestSupplier(Long productId) {
        List<ProductSupplier> links = productSupplierRepo.findBestSuppliersForProduct(productId);

        if (links.isEmpty()) {
            return null;
        }

        return links.get(0).getSupplier();
    }

    @Override
    @Transactional(readOnly = true)
    public Long getActiveAlertCount() {
        Long businessId = requireBusinessId();
        return reorderAlertRepo.countActiveByBusinessId(businessId);
    }

    private ReorderAlertResponse mapToResponse(ReorderAlert alert) {
        return ReorderAlertResponse.builder()
                .id(alert.getId())
                .productId(alert.getProduct().getId())
                .productName(alert.getProduct().getName())
                .productSku(alert.getProduct().getSku())
                .batchId(alert.getBatch() != null ? alert.getBatch().getId() : null)
                .batchNumber(alert.getBatch() != null ? alert.getBatch().getBatchNumber() : null)
                .currentStock(alert.getCurrentStock())
                .reorderPoint(alert.getReorderPoint())
                .suggestedQty(alert.getSuggestedQty())
                .suggestedSupplierId(alert.getSuggestedSupplier() != null ? alert.getSuggestedSupplier().getId() : null)
                .suggestedSupplierName(alert.getSuggestedSupplier() != null ? alert.getSuggestedSupplier().getName() : "N/A")
                .dailyUsageRate(alert.getDailyUsageRate())
                .triggeredAt(alert.getTriggeredAt())
                .status(alert.getStatus().name())
                .build();
    }
}
