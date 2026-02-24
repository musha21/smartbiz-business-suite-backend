package com.example.smartBiz.service;

import com.example.smartBiz.dto.PlanLimitUpsertDto;

import java.util.List;
import java.util.Map;

public interface PlanLimitService {

    /** Upsert (insert-or-update) a list of limits for a plan */
    void upsertLimits(Long planId, List<PlanLimitUpsertDto> limits);

    /** Get all limits for a plan as a Map<key, value> */
    Map<String, Long> getLimitsMap(Long planId);

    /**
     * Get a single limit value for a plan by key.
     * Returns defaultValue if the key doesn't exist.
     * Returns -1 if the key exists with value -1 (unlimited).
     */
    Long getLimitValueOrDefault(Long planId, String limitKey, Long defaultValue);
}
