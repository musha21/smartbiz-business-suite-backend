package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthTokenWrapperDto {
    private AuthResponseDto responseDto;
    private String refreshToken;
}
