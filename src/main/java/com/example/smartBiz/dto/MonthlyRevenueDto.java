package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MonthlyRevenueDto {
    private int year;
    private int month;
    private double paidRevenue;
}
