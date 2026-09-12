package com.jobscheduler.job_scheduler.dto.response;

import com.jobscheduler.job_scheduler.entity.ExecutionStatus;
import com.jobscheduler.job_scheduler.entity.JobTaskType;

import java.time.LocalDateTime;

public record JobExecutionResponse(
        Long id,
        String idempotencyKey,
        Long jobId,
        JobTaskType taskType,
        String taskPayload,
        ExecutionStatus status,
        Integer attemptNumber,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String errorMessage,
        LocalDateTime createdAt
) {
}