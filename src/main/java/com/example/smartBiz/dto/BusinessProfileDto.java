package com.example.smartBiz.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessProfileDto {
    private String businessName;
    private String ownerName;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
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
