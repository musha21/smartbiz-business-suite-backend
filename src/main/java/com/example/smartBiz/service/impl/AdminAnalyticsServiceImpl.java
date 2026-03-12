package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.*;
import com.example.smartBiz.repository.*;
import com.example.smartBiz.service.AdminAnalyticsService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private final BusinessRepo businessRepo;
    private final CustomerRepo customerRepo;
    private final InvoiceRepo invoiceRepo;
    private final ProductRepo productRepo;
    private final UsageCounterRepo usageCounterRepo;
    private final AiUsageLogRepo aiUsageLogRepo;
    private final SubscriptionRepo subscriptionRepo;

    // Simple cache for stats with 5 min TTL
    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> cacheExpiry = new ConcurrentHashMap<>();
    private static final int CACHE_TTL_MINUTES = 5;

    public AdminAnalyticsServiceImpl(BusinessRepo businessRepo, CustomerRepo customerRepo,
                                     InvoiceRepo invoiceRepo, ProductRepo productRepo,
                                     UsageCounterRepo usageCounterRepo, AiUsageLogRepo aiUsageLogRepo,
                                     SubscriptionRepo subscriptionRepo) {
        this.businessRepo = businessRepo;
        this.customerRepo = customerRepo;
        this.invoiceRepo = invoiceRepo;
        this.productRepo = productRepo;
        this.usageCounterRepo = usageCounterRepo;
        this.aiUsageLogRepo = aiUsageLogRepo;
        this.subscriptionRepo = subscriptionRepo;
    }

    @Override
    public PlanAnalyticsDto getPlanAnalytics() {
        List<Object[]> results = businessRepo.countBusinessesGroupByPlan();
        
        // Calculate sum of counts from results to ensure percentages are consistent
        long sumOfCounts = results.stream()
                .mapToLong(res -> res[1] != null ? ((Number) res[1]).longValue() : 0L)
                .sum();

        List<PlanAnalyticsDto.PlanCount> plans = results.stream()
                .map(res -> {
                    String name = res[0] != null ? res[0].toString() : "Unknown";
                    long count = res[1] != null ? ((Number) res[1]).longValue() : 0L;
                    double percentage = sumOfCounts > 0 ? (count * 100.0 / sumOfCounts) : 0.0;
                    return new PlanAnalyticsDto.PlanCount(name, count, Math.round(percentage * 100.0) / 100.0);
                }).toList();

        return PlanAnalyticsDto.builder()
                .totalBusinesses(sumOfCounts)
                .plans(plans)
                .build();
    }

    @Override
    public List<WeeklyActivityDto> getWeeklyActivity() {
        List<WeeklyActivityDto> activity = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 6; i >= 0; i--) {
            LocalDateTime start = now.minusDays(i).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime end = start.plusDays(1).minusNanos(1);

            Long registrations = businessRepo.countByCreatedAtBetween(start, end);
            Long activations = subscriptionRepo.countByCreatedAtBetween(start, end);

            activity.add(new WeeklyActivityDto(
                    start.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    registrations,
                    activations
            ));
        }

        return activity;
    }

    @Override
    public SystemStatsDto getSystemStats() {
        String cacheKey = "system_stats";
        if (isCacheValid(cacheKey)) {
            return (SystemStatsDto) cache.get(cacheKey);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfCurrentMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime startOfPrevMonth = startOfCurrentMonth.minusMonths(1);
        LocalDateTime endOfPrevMonth = startOfCurrentMonth.minusNanos(1);

        SystemStatsDto stats = SystemStatsDto.builder()
                .customers(calculateStatDetail(
                        customerRepo.count(),
                        customerRepo.countByCreatedAtBetween(startOfCurrentMonth, now),
                        customerRepo.countByCreatedAtBetween(startOfPrevMonth, endOfPrevMonth)
                ))
                .businesses(calculateStatDetail(
                        businessRepo.count(),
                        businessRepo.countByCreatedAtBetween(startOfCurrentMonth, now),
                        businessRepo.countByCreatedAtBetween(startOfPrevMonth, endOfPrevMonth)
                ))
                .invoices(calculateStatDetail(
                        invoiceRepo.count(),
                        invoiceRepo.countByInvoiceDateBetween(startOfCurrentMonth, now),
                        invoiceRepo.countByInvoiceDateBetween(startOfPrevMonth, endOfPrevMonth)
                ))
                .products(calculateStatDetail(
                        productRepo.countAllActive(),
                        0L, // Products don't have createdAt, so trend is 0 for now or needs entity update
                        0L
                ))
                .aiCreditsUsed(calculateStatDetail(
                        Optional.ofNullable(usageCounterRepo.sumTotalAiCount()).orElse(0L),
                        Optional.ofNullable(usageCounterRepo.sumTotalAiCountByYearMonth(formatYearMonth(now))).orElse(0L),
                        Optional.ofNullable(usageCounterRepo.sumTotalAiCountByYearMonth(formatYearMonth(now.minusMonths(1)))).orElse(0L)
                ))
                .systemUptime("99.9%") // Placeholder
                .build();

        cache.put(cacheKey, stats);
        cacheExpiry.put(cacheKey, LocalDateTime.now().plusMinutes(CACHE_TTL_MINUTES));
        return stats;
    }

    @Override
    public AiAnalyticsDto getAiAnalytics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        Long totalUsed = Optional.ofNullable(usageCounterRepo.sumTotalAiCount()).orElse(0L);

        List<Object[]> businessUsageRaw = usageCounterRepo.sumAiCountGroupByBusiness();
        List<AiAnalyticsDto.BusinessUsage> usagePerBusiness = businessUsageRaw.stream()
                .map(res -> new AiAnalyticsDto.BusinessUsage((String) res[0], (Long) res[1]))
                .toList();

        List<Object[]> dailyUsageRaw = aiUsageLogRepo.sumUsageGroupByDay(sevenDaysAgo, now);
        List<AiAnalyticsDto.DailyUsage> usagePerDay = dailyUsageRaw.stream()
                .map(res -> new AiAnalyticsDto.DailyUsage((String) res[0], (Long) res[1]))
                .toList();

        List<Object[]> topUsersRaw = aiUsageLogRepo.sumUsageGroupByUser(startOfMonth, now);
        List<AiAnalyticsDto.TopAiUser> topAiUsers = topUsersRaw.stream()
                .map(res -> new AiAnalyticsDto.TopAiUser((String) res[0], (Long) res[1]))
                .toList();
        
        return AiAnalyticsDto.builder()
                .totalAiCreditsUsed(totalUsed)
                .usagePerBusiness(usagePerBusiness)
                .usagePerDay(usagePerDay)
                .topAiUsers(topAiUsers)
                .build();
    }

    private SystemStatsDto.StatDetail calculateStatDetail(Long currentTotal, Long currentMonthCount, Long prevMonthCount) {
        String trend = "0%";
        String direction = "up";

        if (prevMonthCount > 0) {
            double change = ((currentMonthCount - prevMonthCount) * 100.0) / prevMonthCount;
            trend = String.format("%s%.1f%%", change >= 0 ? "+" : "", change);
            direction = change >= 0 ? "up" : "down";
        } else if (currentMonthCount > 0) {
            trend = "+100%";
            direction = "up";
        }

        return SystemStatsDto.StatDetail.builder()
                .value(currentTotal)
                .trend(trend)
                .direction(direction)
                .build();
    }

    private boolean isCacheValid(String key) {
        LocalDateTime expiry = cacheExpiry.get(key);
        return expiry != null && LocalDateTime.now().isBefore(expiry);
    }

    private String formatYearMonth(LocalDateTime dateTime) {
        return String.format("%04d-%02d", dateTime.getYear(), dateTime.getMonthValue());
    }
}
