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
public class AiAnalyticsDto {
    private Long totalAiCreditsUsed;
    private List<BusinessUsage> usagePerBusiness;
    private List<DailyUsage> usagePerDay;
    private List<TopAiUser> topAiUsers;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BusinessUsage {
        private String businessName;
        private Long creditsUsed;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyUsage {
        private String day;
        private Long creditsUsed;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopAiUser {
        private String userName;
        private Long creditsUsed;
    }
}
