package com.jobscheduler.job_scheduler.dto.request;

import com.jobscheduler.job_scheduler.entity.JobPriority;
import com.jobscheduler.job_scheduler.entity.JobScheduleType;
import com.jobscheduler.job_scheduler.entity.JobTaskType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record CreateJobRequest(

        @NotBlank(message = "Job name is required")
        @Size(max = 200, message = "Job name must not exceed 200 characters")
        String name,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        @NotNull(message = "Job type is required")
        JobScheduleType scheduleType,

        @NotNull(message = "Task type is required")
        JobTaskType taskType,

        LocalDateTime scheduledAt,

        String scheduleExpression,

        @NotNull(message = "Task payload is required")
        JsonNode taskPayload,

        @NotNull(message = "Max retries is required")
        @Min(value = 0, message = "Max retries cannot be negative")
        Integer maxRetries,

        @NotNull(message = "Job Priority is required")
        JobPriority priority

) {
}