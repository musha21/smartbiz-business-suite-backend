package com.example.smartBiz.dto;

import lombok.Data;

import java.util.List;

@Data
public class InvoiceCreateRequestDto {
    private Long customerId;
    private List<InvoiceItemRequestDto> items;


}
