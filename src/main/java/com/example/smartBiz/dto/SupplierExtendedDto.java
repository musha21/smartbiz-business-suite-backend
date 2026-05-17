package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierExtendedDto {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private Boolean archived;

    // Extended fields for procurement
    private String paymentTerms;
    private Integer leadTimeDays;
    private Integer moq;
    private BigDecimal reliabilityScore;
    private Integer lateDeliveryCount;
    private String taxId;
    private String bankAccount;
    private String currency;
    private String notes;
}
