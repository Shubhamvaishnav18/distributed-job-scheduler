package com.jobscheduler.job_scheduler.dto.response;

import com.jobscheduler.job_scheduler.entity.ExecutionStatus;

import java.time.LocalDateTime;

public record JobExecutionHistoryResponse(
        Long id,
        Long executionId,
        Integer attemptNumber,
        ExecutionStatus status,
        String errorMessage,
        LocalDateTime createdAt
) {
}
