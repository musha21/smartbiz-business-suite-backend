package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.SupplierDto;
import com.example.smartBiz.service.SupplierService;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class SupplierServiceImpl implements SupplierService {
    @Override
    public SupplierDto createSupplier(SupplierDto supplierDto) {
        return null;
    }

    @Override
    public SupplierDto updateSupplier(Long id, SupplierDto supplierDto) {
        return null;
    }

    @Override
    public void deleteSupplier(Long id) {

    }

    @Override
    public SupplierDto getSupplierById(Long id) {
        return null;
    }

    @Override
    public List<SupplierDto> getAllSuppliers() {
        return List.of();
    }
}
