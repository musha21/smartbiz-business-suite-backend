package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.InvoiceListDto;
import com.example.smartBiz.entity.Invoice;

import com.example.smartBiz.enums.InvoiceStatus;
import com.example.smartBiz.repository.InvoiceRepo;
import com.example.smartBiz.service.InvoiceListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InvoiceListServiceImpl implements InvoiceListService {

    private final InvoiceRepo invoiceRepository;

    @Autowired
    public InvoiceListServiceImpl(InvoiceRepo invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }


    @Override
    public List<InvoiceListDto> getAllInvoices(String status, LocalDateTime from, LocalDateTime to, String q) {
        InvoiceStatus st = parseStatus(status);
        return invoiceRepository.filterInvoices(st, from, to, q)
                .stream().map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<InvoiceListDto> getInvoicesByCustomer(Long customerId, String status, LocalDateTime from, LocalDateTime to, String q) {
        InvoiceStatus st = parseStatus(status);
        return invoiceRepository.filterInvoicesByCustomer(customerId, st, from, to, q)
                .stream().map(this::toDto)
                .collect(Collectors.toList());
    }

    private InvoiceStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        return InvoiceStatus.valueOf(status.toUpperCase()); // "paid" -> PAID
    }

    private InvoiceListDto toDto(Invoice inv) {
        InvoiceListDto dto = new InvoiceListDto();
        dto.setId(inv.getId());
        dto.setInvoiceNumber(inv.getInvoiceNumber());
        dto.setInvoiceDate(inv.getInvoiceDate());
        dto.setStatus(inv.getStatus().name());
        dto.setTotalAmount(inv.getTotalAmount());

        dto.setCustomerId(inv.getCustomer().getId());
        dto.setCustomerName(inv.getCustomer().getName());
        return dto;
    }
}
