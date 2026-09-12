package com.jobscheduler.job_scheduler.service.impl;

import com.jobscheduler.job_scheduler.entity.JobPriority;
import com.jobscheduler.job_scheduler.service.JobQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobQueueServiceImpl implements JobQueueService {

    private static final String QUEUE_NAME =
            "job-priority-queue";

    private final StringRedisTemplate redisTemplate;

    private int getPriorityScore(JobPriority priority) {

        return switch (priority) {
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 3;
        };
    }

    @Override
    public void enqueue(
            Long executionId,
            JobPriority priority
    ) {

        int score =
                getPriorityScore(priority);

        redisTemplate
                .opsForZSet()
                .add(
                        QUEUE_NAME,
                        executionId.toString(),
                        score
                );
    }

    @Override
    public Long dequeue() {

        ZSetOperations.TypedTuple<String> tuple =
                redisTemplate
                        .opsForZSet()
                        .popMin(QUEUE_NAME);

        if (tuple == null) {
            return null;
        }

        return Long.valueOf(
                tuple.getValue()
        );
    }
}