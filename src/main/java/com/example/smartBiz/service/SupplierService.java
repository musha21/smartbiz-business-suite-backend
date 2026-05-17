package com.example.smartBiz.service;

import com.example.smartBiz.dto.SupplierDto;
import com.example.smartBiz.dto.SupplierExtendedDto;

import java.util.List;

public interface SupplierService {
    SupplierDto createSupplier(SupplierDto supplierDto);

    SupplierDto updateSupplier(Long id, SupplierDto supplierDto);

    void deleteSupplier(Long id);

    SupplierDto getSupplierById(Long id);

    List<SupplierDto> getAllSuppliers();

    List<SupplierDto> getArchivedSuppliers();

    void archiveSupplier(Long id);

    void restoreSupplier(Long id);

    // Extended methods for procurement
    SupplierExtendedDto createSupplierExtended(SupplierExtendedDto supplierDto);

    SupplierExtendedDto updateSupplierExtended(Long id, SupplierExtendedDto supplierDto);

    SupplierExtendedDto getSupplierExtendedById(Long id);

    List<SupplierExtendedDto> getAllSuppliersExtended();
}
