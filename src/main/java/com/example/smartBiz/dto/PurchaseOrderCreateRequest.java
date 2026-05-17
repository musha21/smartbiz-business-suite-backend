package com.example.smartBiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderCreateRequest {

    @NotNull(message = "Supplier ID is required")
    private Long supplierId;

    private LocalDate orderDate;

    private LocalDate expectedDelivery;

    private String notes;

    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<POLineItemRequestDto> items;

    private Boolean allowUnlinkedSupplier;
}
