package com.jobscheduler.job_scheduler.service;

import com.jobscheduler.job_scheduler.dto.response.JobExecutionHistoryResponse;
import com.jobscheduler.job_scheduler.entity.ExecutionStatus;
import com.jobscheduler.job_scheduler.entity.JobExecution;

import java.util.List;

public interface JobExecutionHistoryService {

    void record(
            JobExecution execution,
            ExecutionStatus status,
            String errorMessage
    );

    List<JobExecutionHistoryResponse> getHistory(
            Long executionId
    );
}
