package com.jobscheduler.job_scheduler.service.idempotency;

public interface IdempotencyService {

    boolean alreadyProcessed(String idempotencyKey);

    void markProcessed(String idempotencyKey);

    boolean claim(String idempotencyKey);
}
