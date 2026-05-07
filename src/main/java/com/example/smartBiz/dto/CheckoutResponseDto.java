package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResponseDto {

    private String merchantId;
    private String orderId;
    private String itemsDescription;
    private String currency;
    private String amountFormatted;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String country;
    private String notifyUrl;
    private String returnUrl;
    private String cancelUrl;
    private String hash;
    private String checkoutUrl;
}
