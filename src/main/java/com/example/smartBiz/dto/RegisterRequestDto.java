package com.example.smartBiz.dto;

import com.example.smartBiz.entity.Business;
import lombok.Data;

@Data
public class RegisterRequestDto {
    private String name;
    private String email;
    private String password;
    private String businessName; // ✅ required



}
