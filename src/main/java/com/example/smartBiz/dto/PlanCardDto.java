package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanCardDto {
    private Long id;
    private String name;
    private Double price;
    private String durationType; // MONTHLY/YEARLY
    private String description;
    private boolean active;
    private java.util.Map<String, Long> limits;
}
