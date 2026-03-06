package com.example.smartBiz.service.impl;

import com.example.smartBiz.entity.UsageCounter;
import com.example.smartBiz.repository.UsageCounterRepo;
import com.example.smartBiz.service.UsageCounterService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class UsageCounterServiceImpl implements UsageCounterService {

    private final UsageCounterRepo usageCounterRepo;

    private static final DateTimeFormatter YM_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    public UsageCounterServiceImpl(UsageCounterRepo usageCounterRepo) {
        this.usageCounterRepo = usageCounterRepo;
    }

    private String currentYearMonth() {
        return LocalDate.now().format(YM_FORMATTER);
    }

    @Override
    public Long getThisMonthInvoiceCount(Long businessId) {
        String ym = currentYearMonth();
        return usageCounterRepo.findByBusinessIdAndYearMonth(businessId, ym)
                .map(UsageCounter::getInvoiceCount)
                .orElse(0L);
    }

    @Override
    @Transactional
    public void incrementInvoiceCount(Long businessId) {
        String ym = currentYearMonth();
        UsageCounter counter = usageCounterRepo.findByBusinessIdAndYearMonth(businessId, ym)
                .orElseGet(() -> {
                    UsageCounter c = new UsageCounter();
                    c.setBusinessId(businessId);
                    c.setYearMonth(ym);
                    c.setInvoiceCount(0L);
                    c.setAiCount(0L);
                    return c;
                });

        counter.setInvoiceCount(counter.getInvoiceCount() + 1);
        usageCounterRepo.save(counter);
    }

    @Override
    public Long getThisMonthAiCount(Long businessId) {
        String ym = currentYearMonth();
        return usageCounterRepo.findByBusinessIdAndYearMonth(businessId, ym)
                .map(UsageCounter::getAiCount)
                .orElse(0L);
    }

    @Override
    @Transactional
    public void incrementAiCount(Long businessId) {
        String ym = currentYearMonth();
        UsageCounter counter = usageCounterRepo.findByBusinessIdAndYearMonth(businessId, ym)
                .orElseGet(() -> {
                    UsageCounter c = new UsageCounter();
                    c.setBusinessId(businessId);
                    c.setYearMonth(ym);
                    c.setInvoiceCount(0L);
                    c.setAiCount(0L);
                    return c;
                });

        counter.setAiCount(counter.getAiCount() + 1);
        usageCounterRepo.save(counter);
    }
}
