package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.*;
import com.example.smartBiz.enums.InvoiceStatus;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.*;
import com.example.smartBiz.security.RequestContext;
import com.example.smartBiz.service.InvoiceService;
import com.example.smartBiz.service.PlanLimitService;
import com.example.smartBiz.service.SubscriptionService;
import com.example.smartBiz.service.UsageCounterService;
import com.example.smartBiz.entity.*;
import com.example.smartBiz.enums.InvoiceStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepo invoiceRepository;
    private final CustomerRepo customerRepository;
    private final ProductRepo productRepo;
    private final ProductBatchRepo batchRepo;
    private final RequestContext requestContext;

    // ─── Subscription enforcement ────────────────────────
    private final SubscriptionService subscriptionService;
    private final UsageCounterService usageCounterService;

    public InvoiceServiceImpl(
            InvoiceRepo invoiceRepository,
            CustomerRepo customerRepository,
            ProductRepo productRepo,
            ProductBatchRepo batchRepo,
            RequestContext requestContext,
            SubscriptionService subscriptionService,
            UsageCounterService usageCounterService) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.productRepo = productRepo;
        this.batchRepo = batchRepo;
        this.requestContext = requestContext;
        this.subscriptionService = subscriptionService;
        this.usageCounterService = usageCounterService;
    }

    private Long requireBusinessId() {
        Long businessId = requestContext.getBusinessId();
        if (businessId == null)
            throw new RuntimeException("Business context missing (JWT required)");
        return businessId;
    }

    private Customer requireOwnedCustomer(Long customerId, Long businessId) {
        Customer c = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if (c.getBusinessId() == null || !c.getBusinessId().equals(businessId)) {
            throw new ResourceNotFoundException("Access denied: customer not in your business");
        }
        return c;
    }

    private Products requireOwnedProduct(Long productId, Long businessId) {
        Products p = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        if (p.getBusinessId() == null || !p.getBusinessId().equals(businessId)) {
            throw new ResourceNotFoundException("Access denied: product not in your business");
        }
        return p;
    }

    private ProductBatch requireOwnedBatch(Long batchId, Long businessId, Long expectedProductId) {
        ProductBatch batch = batchRepo.findByIdAndBusinessId(batchId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));

        if (batch.getProduct() == null || batch.getProduct().getId() == null) {
            throw new ResourceNotFoundException("Batch product missing");
        }
        if (!batch.getProduct().getId().equals(expectedProductId)) {
            throw new ResourceNotFoundException("Batch does not belong to the selected product");
        }
        return batch;
    }

    private Invoice requireOwnedInvoice(Long invoiceId, Long businessId) {
        Invoice inv = invoiceRepository.findInvoiceWithItems(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (inv.getBusinessId() == null || !inv.getBusinessId().equals(businessId)) {
            throw new ResourceNotFoundException("Access denied: invoice not in your business");
        }
        return inv;
    }

    private String generateInvoiceNumber() {
        return "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // ✅ Keep Products.stock_qty synced with batches (MVP friendly)
    private void syncProductStockFromBatches(Products product, Long businessId) {
        int sum = batchRepo.findByBusinessIdAndProduct_IdOrderByCreatedAtDesc(businessId, product.getId())
                .stream()
                .map(b -> b.getQtyAvailable() == null ? 0 : b.getQtyAvailable())
                .reduce(0, Integer::sum);

        product.setStock_qty(sum);
        productRepo.save(product);
    }

    @Override
    @Transactional
    public InvoiceResponseDto createInvoice(InvoiceCreateRequestDto request) {

        Long businessId = requireBusinessId();

        // ─── PLAN LIMIT ENFORCEMENT ──────────────────────
        // 1) Refresh expiry (mark expired if endAt has passed)
        subscriptionService.refreshExpiryIfNeeded(businessId);

        // 2) Check active subscription exists
        MySubscriptionDto mySub = subscriptionService.getMySubscription(businessId);
        if ("NONE".equals(mySub.getStatus())) {
            throw new RuntimeException("No active subscription. Contact admin to assign a plan.");
        }

        // 3) Check INVOICES_PER_MONTH limit
        Long limit = mySub.getLimits() != null
                ? mySub.getLimits().getOrDefault("INVOICES_PER_MONTH", -1L)
                : -1L;

        if (limit != -1 && mySub.getInvoicesUsed() >= limit) {
            throw new ResourceNotFoundException(
                    "Invoice limit reached (" + mySub.getInvoicesUsed() + "/" + limit
                            + "). Upgrade your plan or contact admin.");
        }
        // ─── END PLAN LIMIT ENFORCEMENT ──────────────────

        if (request.getCustomerId() == null)
            throw new ResourceNotFoundException("customerId is required");
        if (request.getItems() == null || request.getItems().isEmpty())
            throw new ResourceNotFoundException("items are required");

        Customer customer = requireOwnedCustomer(request.getCustomerId(), businessId);

        Invoice invoice = new Invoice();
        invoice.setBusinessId(businessId);
        invoice.setCustomer(customer);
        invoice.setInvoiceDate(LocalDateTime.now());
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setInvoiceNumber(generateInvoiceNumber());

        double grandTotal = 0.0;

        for (InvoiceItemRequestDto itemReq : request.getItems()) {

            if (itemReq.getProductId() == null)
                throw new ResourceNotFoundException("productId is required in items");
            if (itemReq.getBatchId() == null)
                throw new ResourceNotFoundException("batchId is required in items");
            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0)
                throw new ResourceNotFoundException("quantity must be > 0");

            Products product = requireOwnedProduct(itemReq.getProductId(), businessId);
            ProductBatch batch = requireOwnedBatch(itemReq.getBatchId(), businessId, product.getId());

            int qty = itemReq.getQuantity();
            int batchQty = batch.getQtyAvailable() == null ? 0 : batch.getQtyAvailable();

            if (batchQty < qty) {
                throw new ResourceNotFoundException("Insufficient batch stock. Batch: " + batch.getBatchNumber());
            }

            // ✅ reduce batch stock (source of truth)
            batch.setQtyAvailable(batchQty - qty);
            batchRepo.save(batch);

            // ✅ keep product.stock_qty correct (sum of batches)
            syncProductStockFromBatches(product, businessId);

            double unitPrice = product.getPrice() == null ? 0.0 : product.getPrice();
            double lineTotal = unitPrice * qty;
            grandTotal += lineTotal;

            InvoiceItem invoiceItem = new InvoiceItem();
            invoiceItem.setInvoice(invoice);
            invoiceItem.setProduct(product);
            invoiceItem.setBatch(batch);
            invoiceItem.setQuantity(qty);
            invoiceItem.setUnitPrice(unitPrice);
            invoiceItem.setLineTotal(lineTotal);

            invoice.getItems().add(invoiceItem);
        }

        invoice.setTotalAmount(grandTotal);

        Invoice saved = invoiceRepository.save(invoice);

        // ─── INCREMENT USAGE COUNTER ─────────────────────
        usageCounterService.incrementInvoiceCount(businessId);

        return mapToResponse(saved);
    }

    @Override
    public InvoiceResponseDto getInvoiceById(Long id) {
        Long businessId = requireBusinessId();
        Invoice invoice = requireOwnedInvoice(id, businessId);
        return mapToResponse(invoice);
    }

    @Override
    public InvoiceResponseDto getInvoiceByNumber(String invoiceNumber) {
        Long businessId = requireBusinessId();

        Invoice invoice = invoiceRepository
                .findByInvoiceNumberAndBusinessId(invoiceNumber, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        return mapToResponse(invoice);
    }

    @Override
    public void updateInvoiceStatus(Long id, InvoiceStatusUpdateDto invoiceStatus) {
        Long businessId = requireBusinessId();
        Invoice invoice = requireOwnedInvoice(id, businessId);

        if (invoiceStatus.getStatus() == null || invoiceStatus.getStatus().isBlank()) {
            throw new ResourceNotFoundException("Status is required");
        }

        InvoiceStatus newStatus;
        try {
            newStatus = InvoiceStatus.valueOf(invoiceStatus.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Invalid status. Use PAID or UNPAID");
        }

        invoice.setStatus(newStatus);
        invoiceRepository.save(invoice);
    }

    private InvoiceResponseDto mapToResponse(Invoice invoice) {
        InvoiceResponseDto dto = new InvoiceResponseDto();
        dto.setId(invoice.getId());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setInvoiceDate(invoice.getInvoiceDate());
        dto.setStatus(invoice.getStatus() != null ? invoice.getStatus().name() : null);
        dto.setTotalAmount(invoice.getTotalAmount());

        dto.setCustomerId(invoice.getCustomer().getId());
        dto.setCustomerName(invoice.getCustomer().getName());

        List<InvoiceItemResponseDto> itemDtos = invoice.getItems().stream().map(it -> {
            InvoiceItemResponseDto i = new InvoiceItemResponseDto();
            i.setProductId(it.getProduct().getId());
            i.setProductName(it.getProduct().getName());

            if (it.getBatch() != null) {
                i.setBatchId(it.getBatch().getId());
                i.setBatchNumber(it.getBatch().getBatchNumber());
            }

            i.setQuantity(it.getQuantity());
            i.setUnitPrice(it.getUnitPrice());
            i.setLineTotal(it.getLineTotal());
            return i;
        }).collect(Collectors.toList());

        dto.setItems(itemDtos);
        return dto;
    }
}
