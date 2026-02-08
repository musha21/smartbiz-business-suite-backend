package com.example.smartBiz.dto;

import lombok.Data;

@Data
public class RegisterRequestDto {
    private String businessName;
    private String email;
    private String password;
}
