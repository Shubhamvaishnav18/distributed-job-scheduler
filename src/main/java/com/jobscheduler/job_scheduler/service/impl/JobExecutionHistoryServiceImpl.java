package com.jobscheduler.job_scheduler.service.impl;

import com.jobscheduler.job_scheduler.dto.response.JobExecutionHistoryResponse;
import com.jobscheduler.job_scheduler.entity.ExecutionStatus;
import com.jobscheduler.job_scheduler.entity.JobExecution;
import com.jobscheduler.job_scheduler.entity.JobExecutionHistory;
import com.jobscheduler.job_scheduler.repository.JobExecutionHistoryRepository;
import com.jobscheduler.job_scheduler.service.JobExecutionHistoryService;
import lombok.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobExecutionHistoryServiceImpl
        implements JobExecutionHistoryService {

    private final JobExecutionHistoryRepository repository;

    @Override
    public void record(
            JobExecution execution,
            ExecutionStatus status,
            String errorMessage
    ) {

        JobExecutionHistory history =
                JobExecutionHistory.builder()
                        .execution(execution)
                        .attemptNumber(
                                execution.getAttemptNumber()
                        )
                        .status(status)
                        .errorMessage(errorMessage)
                        .createdAt(LocalDateTime.now())
                        .build();

        repository.save(history);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobExecutionHistoryResponse> getHistory(
            Long executionId
    ) {

        return repository
                .findByExecutionIdOrderByCreatedAtAsc(executionId)
                .stream()
                .map(history ->
                        new JobExecutionHistoryResponse(
                                history.getId(),
                                history.getExecution().getId(),
                                history.getAttemptNumber(),
                                history.getStatus(),
                                history.getErrorMessage(),
                                history.getCreatedAt()
                        )
                )
                .toList();
    }
}
