package com.example.smartBiz.service;

public interface UsageCounterService {

    /** Get this month's invoice count for a business */
    Long getThisMonthInvoiceCount(Long businessId);

    /** Increment invoice count for this month (call after invoice saved) */
    void incrementInvoiceCount(Long businessId);

    /** Get this month's AI count for a business */
    Long getThisMonthAiCount(Long businessId);

    /** Increment AI count for this month (call after AI generated) */
    void incrementAiCount(Long businessId);
}
