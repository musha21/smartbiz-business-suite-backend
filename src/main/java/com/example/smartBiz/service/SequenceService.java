package com.example.smartBiz.service;

public interface SequenceService {
    Long getNextValue(Long businessId, String sequenceType, int fiscalYear);
}
