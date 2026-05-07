package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.*;
import com.example.smartBiz.enums.InvoiceStatus;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.*;
import com.example.smartBiz.security.CustomUserPrincipal;
import com.example.smartBiz.service.InvoiceService;
import com.example.smartBiz.service.SubscriptionService;
import com.example.smartBiz.service.UsageCounterService;
import com.example.smartBiz.entity.*;
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
    private final SubscriptionService subscriptionService;
    private final UsageCounterService usageCounterService;

    public InvoiceServiceImpl(
            InvoiceRepo invoiceRepository,
            CustomerRepo customerRepository,
            ProductRepo productRepo,
            ProductBatchRepo batchRepo,
            SubscriptionService subscriptionService,
            UsageCounterService usageCounterService) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.productRepo = productRepo;
        this.batchRepo = batchRepo;
        this.subscriptionService = subscriptionService;
        this.usageCounterService = usageCounterService;
    }

    // ─── Guards ──────────────────────────────────────────────────────────────

    private Long requireBusinessId() {
        CustomUserPrincipal principal = CustomUserPrincipal.getCurrent();
        if (principal == null || principal.getBusinessId() == null)
            throw new ResourceNotFoundException("Business context missing (JWT required)");
        return principal.getBusinessId();
    }

    private Customer requireOwnedCustomer(Long customerId, Long businessId) {
        Customer c = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        if (!businessId.equals(c.getBusinessId()))
            throw new ResourceNotFoundException("Access denied: customer not in your business");
        return c;
    }

    private Products requireOwnedProduct(Long productId, Long businessId) {
        Products p = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        if (!businessId.equals(p.getBusinessId()))
            throw new ResourceNotFoundException("Access denied: product not in your business");
        return p;
    }

    private ProductBatch requireOwnedBatch(Long batchId, Long businessId, Long expectedProductId) {
        ProductBatch batch = batchRepo.findByIdAndBusinessId(batchId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));
        if (batch.getProduct() == null || batch.getProduct().getId() == null)
            throw new ResourceNotFoundException("Batch has no associated product");
        if (!batch.getProduct().getId().equals(expectedProductId))
            throw new ResourceNotFoundException("Batch does not belong to the selected product");
        return batch;
    }

    private Invoice requireOwnedInvoice(Long invoiceId, Long businessId) {
        Invoice inv = invoiceRepository.findInvoiceWithItems(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        if (!businessId.equals(inv.getBusinessId()))
            throw new ResourceNotFoundException("Access denied: invoice not in your business");
        return inv;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String generateInvoiceNumber() {
        return "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Keeps Products.stock_qty in sync by summing all batch quantities.
     * Called after every batch deduction.
     */
    private void syncProductStockFromBatches(Products product, Long businessId) {
        int total = batchRepo
                .findByBusinessIdAndProduct_IdOrderByCreatedAtDesc(businessId, product.getId())
                .stream()
                .mapToInt(b -> b.getQtyAvailable() == null ? 0 : b.getQtyAvailable())
                .sum();

        product.setStock_qty(total);
        productRepo.save(product);
    }

    // ─── Create ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public InvoiceResponseDto createInvoice(InvoiceCreateRequestDto request) {

        Long businessId = requireBusinessId();

        // 1. Refresh subscription expiry if needed
        subscriptionService.refreshExpiryIfNeeded(businessId);

        // 2. Ensure active subscription exists
        MySubscriptionDto mySub = subscriptionService.getMySubscription(businessId);
        if ("NONE".equals(mySub.getStatus()))
            throw new ResourceNotFoundException("No active subscription. Contact admin to assign a plan.");

        // 3. Enforce monthly invoice limit
        long limit = mySub.getLimits() != null
                ? mySub.getLimits().getOrDefault("INVOICES_PER_MONTH", -1L)
                : -1L;

        if (limit != -1 && mySub.getInvoicesUsed() >= limit)
            throw new ResourceNotFoundException(
                    "Invoice limit reached (" + mySub.getInvoicesUsed() + "/" + limit
                            + "). Upgrade your plan or contact admin.");

        // 4. Validate request fields
        if (request.getCustomerId() == null)
            throw new ResourceNotFoundException("customerId is required");
        if (request.getItems() == null || request.getItems().isEmpty())
            throw new ResourceNotFoundException("At least one item is required");

        Customer customer = requireOwnedCustomer(request.getCustomerId(), businessId);

        Invoice invoice = new Invoice();
        invoice.setBusinessId(businessId);
        invoice.setCustomer(customer);
        invoice.setInvoiceDate(LocalDateTime.now());
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setInvoiceNumber(generateInvoiceNumber());

        double grandTotal = 0.0;
        double totalInvoiceDiscount = 0.0;

        for (InvoiceItemRequestDto itemReq : request.getItems()) {

            if (itemReq.getProductId() == null)
                throw new ResourceNotFoundException("productId is required in each item");
            if (itemReq.getBatchId() == null)
                throw new ResourceNotFoundException("batchId is required in each item");
            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0)
                throw new ResourceNotFoundException("quantity must be greater than 0");

            Products product = requireOwnedProduct(itemReq.getProductId(), businessId);
            ProductBatch batch = requireOwnedBatch(itemReq.getBatchId(), businessId, product.getId());

            int qty = itemReq.getQuantity();
            int available = batch.getQtyAvailable() == null ? 0 : batch.getQtyAvailable();

            if (available < qty)
                throw new ResourceNotFoundException(
                        "Insufficient stock in batch: " + batch.getBatchNumber()
                                + " (available: " + available + ", requested: " + qty + ")");

            // Deduct from batch and sync product total
            batch.setQtyAvailable(available - qty);
            batchRepo.save(batch);
            syncProductStockFromBatches(product, businessId);

            double unitPrice = product.getPrice() == null ? 0.0 : product.getPrice();
            double discountPct = itemReq.getDiscountPercentage() != null ? itemReq.getDiscountPercentage() : 0.0;
            double discountAmt = itemReq.getDiscountAmount() != null ? itemReq.getDiscountAmount() : 0.0;

            double subtotal = unitPrice * qty;
            double pctSavings = (subtotal * discountPct) / 100.0;
            double lineTotal = subtotal - pctSavings - discountAmt;

            if (lineTotal < 0) lineTotal = 0.0;

            double itemSavings = subtotal - lineTotal;
            totalInvoiceDiscount += itemSavings;
            grandTotal += lineTotal;

            InvoiceItem invoiceItem = new InvoiceItem();
            invoiceItem.setInvoice(invoice);
            invoiceItem.setProduct(product);
            invoiceItem.setBatch(batch);
            invoiceItem.setQuantity(qty);
            invoiceItem.setUnitPrice(unitPrice);
            invoiceItem.setDiscountPercentage(discountPct);
            invoiceItem.setDiscountAmount(discountAmt);
            invoiceItem.setLineTotal(lineTotal);

            invoice.getItems().add(invoiceItem);
        }

        invoice.setTotalAmount(grandTotal);
        invoice.setTotalDiscount(totalInvoiceDiscount);
        Invoice saved = invoiceRepository.save(invoice);

        usageCounterService.incrementInvoiceCount(businessId);

        return mapToResponse(saved);
    }

    // ─── Read ────────────────────────────────────────────────────────────────

    @Override
    public InvoiceResponseDto getInvoiceById(Long id) {
        Long businessId = requireBusinessId();
        return mapToResponse(requireOwnedInvoice(id, businessId));
    }

    @Override
    public InvoiceResponseDto getInvoiceByNumber(String invoiceNumber) {
        Long businessId = requireBusinessId();
        Invoice invoice = invoiceRepository
                .findByInvoiceNumberAndBusinessId(invoiceNumber, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceNumber));
        return mapToResponse(invoice);
    }

    @Override
    public List<InvoiceListDto> getAllInvoices(Boolean archived) {
        Long businessId = requireBusinessId();
        List<Invoice> invoices;
        if (Boolean.TRUE.equals(archived)) {
            invoices = invoiceRepository.findByBusinessIdAndArchivedTrue(businessId);
        } else {
            invoices = invoiceRepository.findByBusinessIdAndArchivedFalse(businessId);
        }
        return invoices.stream().map(this::mapToListDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void archiveInvoice(Long id) {
        Long businessId = requireBusinessId();
        Invoice invoice = requireOwnedInvoice(id, businessId);
        invoice.setArchived(true);
        invoice.setArchivedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);
    }

    @Override
    @Transactional
    public void restoreInvoice(Long id) {
        Long businessId = requireBusinessId();
        Invoice invoice = requireOwnedInvoice(id, businessId);
        invoice.setArchived(false);
        invoice.setArchivedAt(null);
        invoiceRepository.save(invoice);
    }

    @Override
    @Transactional
    public InvoiceResponseDto updateInvoice(Long id, InvoiceCreateRequestDto request) {
        Long businessId = requireBusinessId();
        Invoice invoice = requireOwnedInvoice(id, businessId);

        if (Boolean.TRUE.equals(invoice.getArchived())) {
            throw new ResourceNotFoundException("Cannot edit an archived invoice. Restore it first.");
        }

        // Simplistic update: replace customer and items
        if (request.getCustomerId() != null) {
            Customer customer = requireOwnedCustomer(request.getCustomerId(), businessId);
            invoice.setCustomer(customer);
        }

        // TODO: Full item/stock update logic if required.

        return mapToResponse(invoiceRepository.save(invoice));
    }

    // ─── Update ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void updateInvoiceStatus(Long id, InvoiceStatusUpdateDto invoiceStatus) {
        Long businessId = requireBusinessId();
        Invoice invoice = requireOwnedInvoice(id, businessId);

        if (invoiceStatus.getStatus() == null || invoiceStatus.getStatus().isBlank())
            throw new ResourceNotFoundException("Status is required");

        InvoiceStatus newStatus;
        try {
            newStatus = InvoiceStatus.valueOf(invoiceStatus.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Invalid status. Allowed values: DRAFT, UNPAID, PAID, CANCELLED");
        }

        invoice.setStatus(newStatus);
        invoiceRepository.save(invoice);
    }

    // ─── Mapping ─────────────────────────────────────────────────────────────

    private InvoiceResponseDto mapToResponse(Invoice invoice) {
        InvoiceResponseDto dto = new InvoiceResponseDto();
        dto.setId(invoice.getId());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setInvoiceDate(invoice.getInvoiceDate());
        dto.setStatus(invoice.getStatus() != null ? invoice.getStatus().name() : null);
        dto.setTotalAmount(invoice.getTotalAmount());
        dto.setTotalDiscount(invoice.getTotalDiscount());

        Customer c = invoice.getCustomer();
        dto.setCustomer(CustomerDto.builder()
                .id(c.getId())
                .name(c.getName())
                .email(c.getEmail())
                .phone(c.getPhone())
                .address(c.getAddress())
                .archived(c.getArchived())
                .build());

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
            i.setDiscountPercentage(it.getDiscountPercentage());
            i.setDiscountAmount(it.getDiscountAmount());
            i.setLineTotal(it.getLineTotal());
            return i;
        }).collect(Collectors.toList());

        dto.setItems(itemDtos);
        dto.setArchived(invoice.getArchived());
        dto.setArchivedAt(invoice.getArchivedAt());
        return dto;
    }

    private InvoiceListDto mapToListDto(Invoice invoice) {
        InvoiceListDto dto = new InvoiceListDto();
        dto.setId(invoice.getId());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setInvoiceDate(invoice.getInvoiceDate());
        dto.setStatus(invoice.getStatus() != null ? invoice.getStatus().name() : null);
        dto.setTotalAmount(invoice.getTotalAmount());
        dto.setTotalDiscount(invoice.getTotalDiscount());
        if (invoice.getCustomer() != null) {
            dto.setCustomerId(invoice.getCustomer().getId());
            dto.setCustomerName(invoice.getCustomer().getName());
        }
        dto.setArchived(invoice.getArchived());
        dto.setArchivedAt(invoice.getArchivedAt());
        return dto;
    }
}