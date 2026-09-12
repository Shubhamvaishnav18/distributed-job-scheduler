package com.jobscheduler.job_scheduler.service.impl;

import com.jobscheduler.job_scheduler.entity.IdempotencyRecord;
import com.jobscheduler.job_scheduler.repository.IdempotencyRecordRepository;
import com.jobscheduler.job_scheduler.service.idempotency.IdempotencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl
        implements IdempotencyService {

    private final IdempotencyRecordRepository repository;

    @Override
    public boolean alreadyProcessed(String idempotencyKey) {
        return repository.existsByIdempotencyKey(idempotencyKey);
    }

    @Override
    @Transactional
    public void markProcessed(String idempotencyKey) {

        if (repository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }

        IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey(idempotencyKey)
                .processedAt(LocalDateTime.now())
                .build();

        repository.save(record);
    }

    @Override
    @Transactional
    public boolean claim(String idempotencyKey) {

        int affectedRows =
                repository.claimIdempotency(idempotencyKey);

        return affectedRows == 1;
    }
}
