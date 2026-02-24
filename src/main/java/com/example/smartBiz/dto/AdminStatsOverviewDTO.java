package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsOverviewDTO {
    private Long totalBusinesses;
    private Long activeBusinesses;
    private Long totalUsers;
    private Long invoicesThisMonth;
    private Double paidRevenueThisMonth;
    private Long paidBusinesses;
    private Long freeBusinesses;
    private Long expiringSoon;
}
