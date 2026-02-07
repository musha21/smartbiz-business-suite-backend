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
import com.example.smartBiz.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepo invoiceRepository;
    private final CustomerRepo customerRepository;
    private final ProductRepo productRepo;

    @Autowired
    public InvoiceServiceImpl(InvoiceRepo invoiceRepository, CustomerRepo customerRepository, ProductRepo productRepo) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.productRepo = productRepo;
    }


    @Override
    public InvoiceResponseDto createInvoice(InvoiceCreateRequestDto request) {

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Invoice invoice = new Invoice();
        invoice.setCustomer(customer);
        invoice.setInvoiceDate(LocalDateTime.now());
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setInvoiceNumber(generateInvoiceNumber());

        double grandTotal = 0.0;

        for (InvoiceItemRequestDto itemReq : request.getItems()) {

            Products product = productRepo.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemReq.getProductId()));

            Integer currentStock = product.getStock_qty();
            if (currentStock == null) currentStock = 0;

            int qty = itemReq.getQuantity();
            if (currentStock < qty) {
                throw new ResourceNotFoundException("Insufficient stock for product: " + product.getName());
            }

            // reduce stock
            product.setStock_qty(product.getStock_qty() - qty);
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

            invoice.getItems().add(invoiceItem);
        }

        invoice.setTotalAmount(grandTotal);

        Invoice saved = invoiceRepository.save(invoice);
        return mapToResponse(saved);
    }

    @Override
    public InvoiceResponseDto getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        return mapToResponse(invoice);
    }

    @Override
    public InvoiceResponseDto getInvoiceByNumber(String invoiceNumber) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        return mapToResponse(invoice);
    }

    @Override
    public void updateInvoiceStatus(Long id, InvoiceStatusUpdateDto invoiceStatus) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (invoiceStatus.getStatus() == null || invoiceStatus.getStatus().isBlank()) {
            throw new ResourceNotFoundException("Status is required");
        }

        InvoiceStatus newStatus;
        try {
            newStatus = InvoiceStatus.valueOf(invoiceStatus.getStatus().toUpperCase()); // paid -> PAID
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status. Use PAID or UNPAID");
        }

        invoice.setStatus(newStatus);
        invoiceRepository.save(invoice);
    }



    private String generateInvoiceNumber() {
        return "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

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
