package com.example.smartBiz.service.impl;

import com.example.smartBiz.entity.BusinessSequence;
import com.example.smartBiz.repository.BusinessSequenceRepo;
import com.example.smartBiz.service.SequenceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SequenceServiceImpl implements SequenceService {

    private final BusinessSequenceRepo sequenceRepo;

    public SequenceServiceImpl(BusinessSequenceRepo sequenceRepo) {
        this.sequenceRepo = sequenceRepo;
    }

    @Override
    @Transactional
    public Long getNextValue(Long businessId, String sequenceType, int fiscalYear) {
        BusinessSequence sequence = sequenceRepo
                .findByBusinessIdAndSequenceTypeAndFiscalYearForUpdate(businessId, sequenceType, fiscalYear)
                .orElseGet(() -> BusinessSequence.builder()
                        .businessId(businessId)
                        .sequenceType(sequenceType)
                        .fiscalYear(fiscalYear)
                        .lastValue(0L)
                        .build());

        Long nextValue = sequence.getLastValue() + 1;
        sequence.setLastValue(nextValue);
        sequenceRepo.save(sequence);

        return nextValue;
    }
}
