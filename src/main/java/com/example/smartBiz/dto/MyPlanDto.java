package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyPlanDto {
    private Long planId;
    private String planName;
    private LocalDateTime expiresAt;
    private String status; // ACTIVE / EXPIRED / NONE
}
