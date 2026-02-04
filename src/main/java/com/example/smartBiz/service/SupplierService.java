package com.example.smartBiz.service;

import com.example.smartBiz.dto.SupplierDto;

import java.util.List;

public interface SupplierService {
    SupplierDto createSupplier(SupplierDto supplierDto);

    SupplierDto updateSupplier(Long id, SupplierDto supplierDto);

    void deleteSupplier(Long id);

    SupplierDto getSupplierById(Long id);

    List<SupplierDto> getAllSuppliers();
}
