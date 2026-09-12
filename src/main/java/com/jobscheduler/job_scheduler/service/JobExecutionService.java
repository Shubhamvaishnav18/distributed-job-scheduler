package com.jobscheduler.job_scheduler.service;

import com.jobscheduler.job_scheduler.dto.response.JobExecutionResponse;

import java.util.List;

public interface JobExecutionService {

    JobExecutionResponse createExecution(Long jobId);

    JobExecutionResponse getExecution(Long executionId);

    JobExecutionResponse startExecution(Long executionId);

    JobExecutionResponse completeExecution(Long executionId);

    JobExecutionResponse failExecution(
            Long executionId,
            String errorMessage
    );

    JobExecutionResponse retryOrFail(
            Long executionId,
            String errorMessage
    );

    JobExecutionResponse claimExecution(
            Long executionId
    );

    List<JobExecutionResponse> getDeadExecutions();

    JobExecutionResponse retryDeadExecution(Long executionId);

    List<JobExecutionResponse> getExecutions(Long jobId);

    JobExecutionResponse cancelExecution(Long executionId);
}