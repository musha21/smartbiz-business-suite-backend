package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.SupplierDto;
import com.example.smartBiz.entity.Supplier;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.SupplierRepo;
import com.example.smartBiz.security.RequestContext;
import com.example.smartBiz.service.SupplierService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepo supplierRepository;
    private final RequestContext requestContext;

    public SupplierServiceImpl(SupplierRepo supplierRepository, RequestContext requestContext) {
        this.supplierRepository = supplierRepository;
        this.requestContext = requestContext;
    }

    // ✅ reduce duplicates
    private Long requireBusinessId() {
        Long businessId = requestContext.getBusinessId();
        if (businessId == null) {
            throw new RuntimeException("Business context missing (JWT required)");
        }
        return businessId;
    }

    private Supplier requireOwnedSupplier(Long id, Long businessId) {
        Supplier s = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id " + id));

        if (s.getBusinessId() == null || !s.getBusinessId().equals(businessId)) {
            throw new RuntimeException("Access denied: supplier not in your business");
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
        Long businessId = requireBusinessId();
        Supplier supplier = requireOwnedSupplier(id, businessId);
        supplierRepository.delete(supplier);
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

        return supplierRepository.findByBusinessId(businessId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }
}
