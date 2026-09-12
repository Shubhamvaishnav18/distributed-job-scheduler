package com.jobscheduler.job_scheduler.service;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
public class JobQueueMetrics {

    private static final String QUEUE_NAME = "job-priority-queue";

    private final MeterRegistry meterRegistry;
    private final StringRedisTemplate redisTemplate;

    @PostConstruct
    public void registerQueueGauge() {

        Gauge.builder(
                        "job.queue.size",
                        redisTemplate,
                        template -> {
                            Long size =
                                    template.opsForZSet().size(QUEUE_NAME);

                            return size != null ? size : 0;
                        }
                )
                .description("Current number of executions waiting in job queue")
                .register(meterRegistry);
    }
}