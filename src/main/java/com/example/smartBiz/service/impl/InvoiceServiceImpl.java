package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.*;
import com.example.smartBiz.entity.Customer;
import com.example.smartBiz.entity.Invoice;
import com.example.smartBiz.entity.InvoiceItem;
import com.example.smartBiz.entity.Products;
import com.example.smartBiz.enums.InvoiceStatus;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.CustomerRepo;
import com.example.smartBiz.repository.InvoiceRepo;
import com.example.smartBiz.repository.ProductRepo;
import com.example.smartBiz.security.RequestContext;
import com.example.smartBiz.service.InvoiceService;
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
    private final RequestContext requestContext;

    public InvoiceServiceImpl(
            InvoiceRepo invoiceRepository,
            CustomerRepo customerRepository,
            ProductRepo productRepo,
            RequestContext requestContext
    ) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.productRepo = productRepo;
        this.requestContext = requestContext;
    }

    // -------------------------
    // Helpers (reduce duplicate)
    // -------------------------
    private Long requireBusinessId() {
        Long businessId = requestContext.getBusinessId();
        if (businessId == null) {
            throw new RuntimeException("Business context missing (JWT required)");
        }
        return businessId;
    }

    private Customer requireOwnedCustomer(Long customerId, Long businessId) {
        Customer c = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if (c.getBusinessId() == null || !c.getBusinessId().equals(businessId)) {
            throw new RuntimeException("Access denied: customer not in your business");
        }
        return c;
    }

    private Products requireOwnedProduct(Long productId, Long businessId) {
        Products p = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        // Your Products entity uses business_id
        if (p.getBusinessId() == null || !p.getBusinessId().equals(businessId)) {
            throw new RuntimeException("Access denied: product not in your business");
        }
        return p;
    }

    private Invoice requireOwnedInvoice(Long invoiceId, Long businessId) {
        Invoice inv = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (inv.getBusinessId() == null || !inv.getBusinessId().equals(businessId)) {
            throw new RuntimeException("Access denied: invoice not in your business");
        }
        return inv;
    }

    private String generateInvoiceNumber() {
        return "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // -------------------------
    // CREATE INVOICE
    // -------------------------
    @Override
    @Transactional
    public InvoiceResponseDto createInvoice(InvoiceCreateRequestDto request) {

        Long businessId = requireBusinessId();

        Customer customer = requireOwnedCustomer(request.getCustomerId(), businessId);

        Invoice invoice = new Invoice();
        invoice.setBusinessId(businessId);
        invoice.setCustomer(customer);
        invoice.setInvoiceDate(LocalDateTime.now());
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setInvoiceNumber(generateInvoiceNumber());

        double grandTotal = 0.0;

        for (InvoiceItemRequestDto itemReq : request.getItems()) {

            Products product = requireOwnedProduct(itemReq.getProductId(), businessId);

            Integer currentStock = product.getStock_qty();
            if (currentStock == null) currentStock = 0;

            int qty = itemReq.getQuantity();
            if (currentStock < qty) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName());
            }

            // reduce stock safely
            product.setStock_qty(currentStock - qty);
            productRepo.save(product);

            double unitPrice = product.getPrice();
            double lineTotal = unitPrice * qty;
            grandTotal += lineTotal;

            InvoiceItem invoiceItem = new InvoiceItem();
            invoiceItem.setInvoice(invoice);
            invoiceItem.setProduct(product);
            invoiceItem.setQuantity(qty);
            invoiceItem.setUnitPrice(unitPrice);
            invoiceItem.setLineTotal(lineTotal);

            // make sure Invoice has items initialized (List)
            invoice.getItems().add(invoiceItem);
        }

        invoice.setTotalAmount(grandTotal);

        Invoice saved = invoiceRepository.save(invoice);
        return mapToResponse(saved);
    }

    // -------------------------
    // GET INVOICE (BY ID)
    // -------------------------
    @Override
    public InvoiceResponseDto getInvoiceById(Long id) {
        Long businessId = requireBusinessId();
        Invoice invoice = requireOwnedInvoice(id, businessId);
        return mapToResponse(invoice);
    }

    // -------------------------
    // GET INVOICE (BY NUMBER)
    // -------------------------
    @Override
    public InvoiceResponseDto getInvoiceByNumber(String invoiceNumber) {
        Long businessId = requireBusinessId();

        Invoice invoice = invoiceRepository
                .findByInvoiceNumberAndBusinessId(invoiceNumber, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        return mapToResponse(invoice);
    }

    // -------------------------
    // UPDATE STATUS (PAID/UNPAID)
    // -------------------------
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
            throw new RuntimeException("Invalid status. Use PAID or UNPAID");
        }

        invoice.setStatus(newStatus);
        invoiceRepository.save(invoice);
    }

//    // -------------------------
//    // LIST INVOICES (BUSINESS)
//    // -------------------------
//    @Override
//    public List<InvoiceResponseDto> getAllInvoices() {
//        Long businessId = requireBusinessId();
//
//        return invoiceRepository.findByBusinessId(businessId)
//                .stream()
//                .map(this::mapToResponse)
//                .toList();
//    }

    // -------------------------
    // LIST INVOICES (BY CUSTOMER)
    // -------------------------
//    @Override
//    public List<InvoiceResponseDto> getInvoicesByCustomer(Long customerId) {
//        Long businessId = requireBusinessId();
//
//        // optional: ensure customer belongs to business
//        requireOwnedCustomer(customerId, businessId);
//
//        return invoiceRepository.findByBusinessIdAndCustomerId(businessId, customerId)
//                .stream()
//                .map(this::mapToResponse)
//                .toList();
//    }

    // -------------------------
    // MAPPING
    // -------------------------
    private InvoiceResponseDto mapToResponse(Invoice invoice) {
        InvoiceResponseDto dto = new InvoiceResponseDto();
        dto.setId(invoice.getId());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setInvoiceDate(invoice.getInvoiceDate());
        dto.setStatus(invoice.getStatus().name());
        dto.setTotalAmount(invoice.getTotalAmount());

        dto.setCustomerId(invoice.getCustomer().getId());
        dto.setCustomerName(invoice.getCustomer().getName());

        List<InvoiceItemResponseDto> itemDtos = invoice.getItems().stream().map(it -> {
            InvoiceItemResponseDto i = new InvoiceItemResponseDto();
            i.setProductId(it.getProduct().getId());
            i.setProductName(it.getProduct().getName());
            i.setQuantity(it.getQuantity());
            i.setUnitPrice(it.getUnitPrice());
            i.setLineTotal(it.getLineTotal());
            return i;
        }).collect(Collectors.toList());

        dto.setItems(itemDtos);
        return dto;
    }
}
