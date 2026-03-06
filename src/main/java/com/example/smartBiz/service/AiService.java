package com.example.smartBiz.service;

import com.example.smartBiz.dto.AiReportResponseDto;
import com.example.smartBiz.dto.AnalyticsResponseDto;
import java.time.LocalDate;

public interface AiService {
    AnalyticsResponseDto getAnalytics(Long businessId, LocalDate from, LocalDate to);

    AiReportResponseDto generateReport(Long businessId, LocalDate from, LocalDate to, String prompt);

    String generateMarketingPost(Long businessId, String topic);

    String generateEmailDraft(Long businessId, String category, String tone, String context);
}
