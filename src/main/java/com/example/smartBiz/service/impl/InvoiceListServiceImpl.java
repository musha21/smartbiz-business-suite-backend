package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.InvoiceListDto;
import com.example.smartBiz.entity.Invoice;
import com.example.smartBiz.enums.InvoiceStatus;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.InvoiceRepo;
import com.example.smartBiz.security.CustomUserPrincipal;
import com.example.smartBiz.service.InvoiceListService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InvoiceListServiceImpl implements InvoiceListService {

    private final InvoiceRepo invoiceRepository;

    public InvoiceListServiceImpl(InvoiceRepo invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    public List<InvoiceListDto> getAllInvoices(String status, LocalDateTime from, LocalDateTime to, String q) {

        CustomUserPrincipal principal = CustomUserPrincipal.getCurrent();
        if (principal == null || principal.getBusinessId() == null) {
            throw new ResourceNotFoundException("Business context missing (JWT required)");
        }
        Long businessId = principal.getBusinessId();

        InvoiceStatus st = parseStatusSafe(status);

        return invoiceRepository.filterInvoices(businessId, st, from, to, normalizeQuery(q))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<InvoiceListDto> getInvoicesByCustomer(Long customerId, String status, LocalDateTime from,
            LocalDateTime to, String q) {

        CustomUserPrincipal principal = CustomUserPrincipal.getCurrent();
        if (principal == null || principal.getBusinessId() == null) {
            throw new ResourceNotFoundException("Business context missing (JWT required)");
        }
        Long businessId = principal.getBusinessId();

        InvoiceStatus st = parseStatusSafe(status);

        return invoiceRepository.filterInvoicesByCustomer(businessId, customerId, st, from, to, normalizeQuery(q))
                .stream()
                .map(this::toDto)
                .toList();
    }

    private String normalizeQuery(String q) {
        if (q == null)
            return null;
        String trimmed = q.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private InvoiceStatus parseStatusSafe(String status) {
        if (status == null || status.isBlank())
            return null;
        try {
            return InvoiceStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // invalid status -> ignore filter instead of crashing
            return null;
        }
    }

    private InvoiceListDto toDto(Invoice inv) {
        InvoiceListDto dto = new InvoiceListDto();
        dto.setId(inv.getId());
        dto.setInvoiceNumber(inv.getInvoiceNumber());
        dto.setInvoiceDate(inv.getInvoiceDate());
        dto.setStatus(inv.getStatus() != null ? inv.getStatus().name() : "UNKNOWN");
        dto.setTotalAmount(inv.getTotalAmount());

        if (inv.getCustomer() != null) {
            dto.setCustomerId(inv.getCustomer().getId());
            dto.setCustomerName(inv.getCustomer().getName());
        }
        dto.setArchived(inv.getArchived());
        dto.setArchivedAt(inv.getArchivedAt());
        return dto;
    }
}
