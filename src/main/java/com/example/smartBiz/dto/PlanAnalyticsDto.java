package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanAnalyticsDto {
    private Long totalBusinesses;
    private List<PlanCount> plans;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanCount {
        private String name;
        private Long count;
        private Double percentage;
    }
}
