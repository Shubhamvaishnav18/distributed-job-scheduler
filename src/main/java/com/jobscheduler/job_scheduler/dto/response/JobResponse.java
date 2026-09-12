package com.jobscheduler.job_scheduler.dto.response;

import com.jobscheduler.job_scheduler.entity.JobPriority;
import com.jobscheduler.job_scheduler.entity.JobScheduleType;
import com.jobscheduler.job_scheduler.entity.JobStatus;
import com.jobscheduler.job_scheduler.entity.JobTaskType;

import java.time.LocalDateTime;

public record JobResponse(
        Long id,
        String name,
        String description,
        JobScheduleType scheduleType,
        JobTaskType taskType,
        String taskPayload,
        JobStatus status,
        String scheduleExpression,
        Integer maxRetries,
        JobPriority priority,
        LocalDateTime nextRunAt,
        Long userId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}