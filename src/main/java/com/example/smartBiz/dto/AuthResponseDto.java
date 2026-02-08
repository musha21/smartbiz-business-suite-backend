package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponseDto {
    private String token;
    private Long userId;
    private Long businessId;
    private String role;
}
