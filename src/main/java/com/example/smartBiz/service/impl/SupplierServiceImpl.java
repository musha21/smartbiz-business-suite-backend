package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.SupplierDto;
import com.example.smartBiz.dto.SupplierExtendedDto;
import com.example.smartBiz.entity.Supplier;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.SupplierRepo;
import com.example.smartBiz.security.CustomUserPrincipal;
import com.example.smartBiz.service.SupplierService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepo supplierRepository;

    public SupplierServiceImpl(SupplierRepo supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    // ✅ reduce duplicates
    private Long requireBusinessId() {
        CustomUserPrincipal principal = CustomUserPrincipal.getCurrent();
        if (principal == null || principal.getBusinessId() == null) {
            throw new ResourceNotFoundException("Business context missing (JWT required)");
        }
        return principal.getBusinessId();
    }

    private Supplier requireOwnedSupplier(Long id, Long businessId) {
        Supplier s = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id " + id));

        if (s.getBusinessId() == null || !s.getBusinessId().equals(businessId)) {
            throw new ResourceNotFoundException("Access denied: supplier not in your business");
        }
        return s;
    }

    private Supplier mapToEntity(SupplierDto dto) {
        Supplier supplier = new Supplier();
        supplier.setName(dto.getName());
        supplier.setEmail(dto.getEmail());
        supplier.setPhone(dto.getPhone());
        supplier.setAddress(dto.getAddress());
        return supplier;
    }

    private SupplierDto mapToDto(Supplier supplier) {
        SupplierDto dto = new SupplierDto();
        dto.setId(supplier.getId());
        dto.setName(supplier.getName());
        dto.setEmail(supplier.getEmail());
        dto.setPhone(supplier.getPhone());
        dto.setAddress(supplier.getAddress());
        dto.setArchived(supplier.getArchived());
        return dto;
    }

    @Override
    public SupplierDto createSupplier(SupplierDto dto) {
        Long businessId = requireBusinessId();

        Supplier supplier = mapToEntity(dto);
        supplier.setBusinessId(businessId);

        return mapToDto(supplierRepository.save(supplier));
    }

    @Override
    public SupplierDto updateSupplier(Long id, SupplierDto dto) {
        Long businessId = requireBusinessId();
        Supplier supplier = requireOwnedSupplier(id, businessId);

        supplier.setName(dto.getName());
        supplier.setEmail(dto.getEmail());
        supplier.setPhone(dto.getPhone());
        supplier.setAddress(dto.getAddress());

        return mapToDto(supplierRepository.save(supplier));
    }

    @Override
    public void deleteSupplier(Long id) {
        archiveSupplier(id);
    }

    @Override
    public List<SupplierDto> getArchivedSuppliers() {
        Long businessId = requireBusinessId();
        return supplierRepository.findByBusinessIdAndArchivedTrue(businessId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    public void archiveSupplier(Long id) {
        Long businessId = requireBusinessId();
        Supplier supplier = requireOwnedSupplier(id, businessId);
        supplier.setArchived(true);
        supplier.setArchivedAt(java.time.LocalDateTime.now());
        supplierRepository.save(supplier);
    }

    @Override
    public void restoreSupplier(Long id) {
        Long businessId = requireBusinessId();
        Supplier supplier = requireOwnedSupplier(id, businessId);
        supplier.setArchived(false);
        supplier.setArchivedAt(null);
        supplierRepository.save(supplier);
    }

    @Override
    public SupplierDto getSupplierById(Long id) {
        Long businessId = requireBusinessId();
        Supplier supplier = requireOwnedSupplier(id, businessId);
        return mapToDto(supplier);
    }

    @Override
    public List<SupplierDto> getAllSuppliers() {
        Long businessId = requireBusinessId();

        return supplierRepository.findByBusinessIdAndArchivedFalse(businessId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    // Extended methods for procurement

    @Override
    public SupplierExtendedDto createSupplierExtended(SupplierExtendedDto dto) {
        Long businessId = requireBusinessId();

        Supplier supplier = mapExtendedToEntity(dto);
        supplier.setBusinessId(businessId);

        return mapExtendedToDto(supplierRepository.save(supplier));
    }

    @Override
    public SupplierExtendedDto updateSupplierExtended(Long id, SupplierExtendedDto dto) {
        Long businessId = requireBusinessId();
        Supplier supplier = requireOwnedSupplier(id, businessId);

        supplier.setName(dto.getName());
        supplier.setEmail(dto.getEmail());
        supplier.setPhone(dto.getPhone());
        supplier.setAddress(dto.getAddress());
        supplier.setPaymentTerms(dto.getPaymentTerms());
        supplier.setLeadTimeDays(dto.getLeadTimeDays());
        supplier.setMoq(dto.getMoq());
        supplier.setTaxId(dto.getTaxId());
        supplier.setBankAccount(dto.getBankAccount());
        supplier.setCurrency(dto.getCurrency());
        supplier.setNotes(dto.getNotes());

        return mapExtendedToDto(supplierRepository.save(supplier));
    }

    @Override
    public SupplierExtendedDto getSupplierExtendedById(Long id) {
        Long businessId = requireBusinessId();
        Supplier supplier = requireOwnedSupplier(id, businessId);
        return mapExtendedToDto(supplier);
    }

    @Override
    public List<SupplierExtendedDto> getAllSuppliersExtended() {
        Long businessId = requireBusinessId();

        return supplierRepository.findByBusinessIdAndArchivedFalse(businessId)
                .stream()
                .map(this::mapExtendedToDto)
                .toList();
    }

    private Supplier mapExtendedToEntity(SupplierExtendedDto dto) {
        Supplier supplier = new Supplier();
        supplier.setName(dto.getName());
        supplier.setEmail(dto.getEmail());
        supplier.setPhone(dto.getPhone());
        supplier.setAddress(dto.getAddress());
        supplier.setPaymentTerms(dto.getPaymentTerms());
        supplier.setLeadTimeDays(dto.getLeadTimeDays());
        supplier.setMoq(dto.getMoq());
        supplier.setTaxId(dto.getTaxId());
        supplier.setBankAccount(dto.getBankAccount());
        supplier.setCurrency(dto.getCurrency());
        supplier.setNotes(dto.getNotes());
        return supplier;
    }

    private SupplierExtendedDto mapExtendedToDto(Supplier supplier) {
        return SupplierExtendedDto.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .email(supplier.getEmail())
                .phone(supplier.getPhone())
                .address(supplier.getAddress())
                .archived(supplier.getArchived())
                .paymentTerms(supplier.getPaymentTerms())
                .leadTimeDays(supplier.getLeadTimeDays())
                .moq(supplier.getMoq())
                .reliabilityScore(supplier.getReliabilityScore())
                .lateDeliveryCount(supplier.getLateDeliveryCount())
                .taxId(supplier.getTaxId())
                .bankAccount(supplier.getBankAccount())
                .currency(supplier.getCurrency())
                .notes(supplier.getNotes())
                .build();
    }
}
