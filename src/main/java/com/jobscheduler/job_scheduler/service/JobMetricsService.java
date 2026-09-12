package com.jobscheduler.job_scheduler.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobMetricsService {

    private final MeterRegistry meterRegistry;

    public void recordSuccess(
            String taskType,
            String worker
    ) {

        meterRegistry
                .counter(
                        "jobs.success",
                        "taskType",
                        taskType,
                        "worker",
                        worker
                )
                .increment();
    }

    public void recordFailure() {

        meterRegistry
                .counter("jobs.failed")
                .increment();
    }

    public void recordRetry() {

        meterRegistry
                .counter("jobs.retried")
                .increment();
    }

    public void recordDead() {

        meterRegistry
                .counter("jobs.dead")
                .increment();
    }

    public void recordCancellation() {

        meterRegistry
                .counter("jobs.cancelled")
                .increment();
    }

    public void recordQueueLag(long lagMillis) {
        meterRegistry
                .timer("job.queue.lag")
                .record(lagMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
}