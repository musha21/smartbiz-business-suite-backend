package com.example.smartBiz.service;

public interface UsageCounterService {

    /** Get this month's invoice count for a business */
    Long getThisMonthInvoiceCount(Long businessId);

    /** Increment invoice count for this month (call after invoice saved) */
    void incrementInvoiceCount(Long businessId);
}
