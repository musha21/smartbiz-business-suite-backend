package com.example.smartBiz.service;

import com.example.smartBiz.dto.*;
import java.util.List;

public interface AdminAnalyticsService {
    PlanAnalyticsDto getPlanAnalytics();
    List<WeeklyActivityDto> getWeeklyActivity();
    SystemStatsDto getSystemStats();
    AiAnalyticsDto getAiAnalytics();
}
