package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessProfileDto {
    private Long id;
    private String businessName;
    private String ownerName;
    private String logo;
    private String email;
    private String phone;
    private String address;
    private String currency;
    private String invoicePrefix;
    private String brandColor;
    private String industry;
    private String country;
    private String brandTagline;
}
