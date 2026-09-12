package com.jobscheduler.job_scheduler.service.impl;

import com.jobscheduler.job_scheduler.dto.response.JobExecutionResponse;
import com.jobscheduler.job_scheduler.entity.*;
import com.jobscheduler.job_scheduler.exception.BusinessRuleException;
import com.jobscheduler.job_scheduler.exception.ResourceNotFoundException;
import com.jobscheduler.job_scheduler.repository.JobExecutionRepository;
import com.jobscheduler.job_scheduler.repository.JobRepository;
import com.jobscheduler.job_scheduler.repository.OutboxRepository;
import com.jobscheduler.job_scheduler.service.JobExecutionHistoryService;
import com.jobscheduler.job_scheduler.service.JobExecutionService;
import com.jobscheduler.job_scheduler.service.JobMetricsService;
import com.jobscheduler.job_scheduler.service.retry.RetryPolicy;
import com.jobscheduler.job_scheduler.service.scheduler.ScheduleCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JobExecutionServiceImpl implements JobExecutionService {

    private final JobExecutionRepository jobExecutionRepository;
    private final JobRepository jobRepository;
    private final RetryPolicy retryPolicy;
    private final OutboxRepository outboxRepository;
    private final JobExecutionHistoryService jobExecutionHistoryService;
    private final JobMetricsService jobMetricsService;
    private final ScheduleCalculator scheduleCalculator;

    @Override
    @Transactional
    public JobExecutionResponse createExecution(Long jobId) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job with id " + jobId + " not found"
                        )
                );

        if (job.getStatus() == JobStatus.CANCELLED) {
            throw new BusinessRuleException(
                    "Cancelled job cannot be executed"
            );
        }

        if (job.getNextRunAt() == null) {
            throw new BusinessRuleException(
                    "Cannot create execution because nextRunAt is null"
            );
        }

        String idempotencyKey =
                "JOB-" + job.getId() + "-" + job.getNextRunAt();

        Optional<JobExecution> existingExecution =
                jobExecutionRepository.findByIdempotencyKey(idempotencyKey);

        if (existingExecution.isPresent()) {

            throw new BusinessRuleException(
                    "Execution already exists for idempotency key: "
                            + idempotencyKey
            );
        }

        JobExecution execution = JobExecution.builder()
                .job(job)
                .status(ExecutionStatus.QUEUED)
                .attemptNumber(1)
                .idempotencyKey(idempotencyKey)
                .build();

        JobExecution savedExecution =
                jobExecutionRepository.save(execution);

        return mapToResponse(savedExecution);
    }

    @Override
    @Transactional(readOnly = true)
    public JobExecutionResponse getExecution(Long executionId) {

        return mapToResponse(
                getExecutionEntity(executionId)
        );
    }

    @Override
    @Transactional
    public JobExecutionResponse startExecution(Long executionId) {

        JobExecution execution = getExecutionEntity(executionId);

        validateTransition(
                execution.getStatus(),
                ExecutionStatus.RUNNING
        );

        execution.setStatus(ExecutionStatus.RUNNING);
        execution.setStartedAt(LocalDateTime.now());

        JobExecution updatedExecution =
                jobExecutionRepository.save(execution);

        return mapToResponse(updatedExecution);
    }

    @Override
    @Transactional
    public JobExecutionResponse completeExecution(Long executionId) {

        JobExecution execution =
                getExecutionEntity(executionId);

        validateTransition(
                execution.getStatus(),
                ExecutionStatus.SUCCESS
        );

        execution.setStatus(ExecutionStatus.SUCCESS);
        execution.setCompletedAt(LocalDateTime.now());

        JobExecution saved =
                jobExecutionRepository.save(execution);

        jobExecutionHistoryService.record(
                saved,
                ExecutionStatus.SUCCESS,
                null
        );

        Job job = execution.getJob();

        switch (job.getScheduleType()) {

            case ONE_TIME -> {

                job.setStatus(JobStatus.COMPLETED);
                job.setNextRunAt(null);
            }

            case CRON, RECURRING -> {

                LocalDateTime currentRunAt =
                        job.getNextRunAt();

                LocalDateTime nextRunAt =
                        scheduleCalculator.calculateNextRunAt(
                                job.getScheduleType(),
                                currentRunAt,
                                job.getScheduleExpression()
                        );

                job.setNextRunAt(nextRunAt);
                job.setStatus(JobStatus.SCHEDULED);
            }
        }

        jobRepository.save(job);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public JobExecutionResponse failExecution(
            Long executionId,
            String errorMessage
    ) {

        JobExecution execution = getExecutionEntity(executionId);

        validateTransition(
                execution.getStatus(),
                ExecutionStatus.FAILED
        );

        execution.setStatus(ExecutionStatus.FAILED);
        execution.setErrorMessage(errorMessage);
        execution.setCompletedAt(LocalDateTime.now());

        JobExecution updatedExecution =
                jobExecutionRepository.save(execution);

        jobMetricsService.recordFailure();

        return mapToResponse(updatedExecution);
    }

    @Override
    @Transactional
    public JobExecutionResponse retryOrFail(
            Long executionId,
            String errorMessage
    ) {

        JobExecution execution =
                getExecutionEntity(executionId);

        validateTransition(
                execution.getStatus(),
                ExecutionStatus.FAILED
        );

        execution.setErrorMessage(errorMessage);

        int currentAttempt =
                execution.getAttemptNumber();

        int maxRetries =
                execution.getJob().getMaxRetries();

        if (currentAttempt <= maxRetries) {

            long delaySeconds =
                    retryPolicy.getDelaySeconds(
                            currentAttempt
                    );

            execution.setStatus(
                    ExecutionStatus.RETRYING
            );

            execution.setNextRetryAt(
                    LocalDateTime.now()
                            .plusSeconds(delaySeconds)
            );

            jobExecutionHistoryService.record(
                    execution,
                    ExecutionStatus.RETRYING,
                    errorMessage
            );

            JobExecution updatedExecution =
                    jobExecutionRepository.save(execution);

            jobMetricsService.recordRetry();

            return mapToResponse(updatedExecution);
        }

        execution.setStatus(
                ExecutionStatus.DEAD
        );

        execution.setCompletedAt(
                LocalDateTime.now()
        );

        execution.setNextRetryAt(null);

        jobExecutionHistoryService.record(
                execution,
                ExecutionStatus.DEAD,
                errorMessage
        );

        JobExecution updatedExecution =
                jobExecutionRepository.save(execution);

        jobMetricsService.recordDead();

        return mapToResponse(updatedExecution);
    }

    @Override
    @Transactional
    public JobExecutionResponse claimExecution(
            Long executionId
    ) {

        int updatedRows =
                jobExecutionRepository.claimExecution(
                        executionId
                );

        if (updatedRows == 0) {

            throw new BusinessRuleException(
                    "Execution " + executionId
                            + " is already claimed or is not QUEUED"
            );
        }

        JobExecution execution =
                jobExecutionRepository.findById(executionId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Execution not found: "
                                                + executionId
                                )
                        );

        jobExecutionHistoryService.record(
                execution,
                ExecutionStatus.RUNNING,
                null
        );

        return mapToResponse(execution);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobExecutionResponse> getDeadExecutions() {

        return jobExecutionRepository
                .findByStatus(ExecutionStatus.DEAD)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public JobExecutionResponse retryDeadExecution(Long executionId) {

        JobExecution execution =
                jobExecutionRepository.findById(executionId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Execution not found: "
                                                + executionId
                                )
                        );

        if (execution.getStatus() != ExecutionStatus.DEAD) {

            throw new BusinessRuleException(
                    "Only DEAD executions can be manually retried"
            );
        }

        execution.setStatus(ExecutionStatus.QUEUED);

        execution.setAttemptNumber(
                execution.getAttemptNumber() + 1
        );

        execution.setNextRetryAt(null);

        execution.setErrorMessage(null);

        execution.setStartedAt(null);

        execution.setCompletedAt(null);

        JobExecution saved =
                jobExecutionRepository.save(execution);

        jobExecutionHistoryService.record(
                saved,
                ExecutionStatus.QUEUED,
                null
        );

        OutboxEvent event = OutboxEvent.builder()
                .eventType("JOB_EXECUTION_RETRY")
                .aggregateId(saved.getId())
                .payload(
                        "{\"executionId\":" +
                                saved.getId() +
                                "}"
                )
                .status(OutboxStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .build();

        outboxRepository.save(event);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public JobExecutionResponse cancelExecution(
            Long executionId
    ) {

        int updatedRows =
                jobExecutionRepository.cancelExecution(
                        executionId
                );

        if (updatedRows == 0) {

            JobExecution execution =
                    jobExecutionRepository.findById(executionId)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Execution not found: "
                                                    + executionId
                                    )
                            );

            throw new BusinessRuleException(
                    "Execution cannot be cancelled from status: "
                            + execution.getStatus()
            );
        }

        JobExecution execution =
                jobExecutionRepository.findById(executionId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Execution not found: "
                                                + executionId
                                )
                        );

        jobExecutionHistoryService.record(
                execution,
                ExecutionStatus.CANCELLED,
                null
        );

        jobMetricsService.recordCancellation();

        return mapToResponse(execution);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobExecutionResponse> getExecutions(
            Long jobId
    ) {

        return jobExecutionRepository
                .findByJobIdOrderByCreatedAtDesc(jobId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private JobExecution getExecutionEntity(Long executionId) {

        return jobExecutionRepository.findByIdWithJob(executionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Execution with id "
                                        + executionId
                                        + " not found"
                        )
                );
    }

    private void validateTransition(
            ExecutionStatus currentStatus,
            ExecutionStatus targetStatus
    ) {

        boolean valid = switch (currentStatus) {

            case QUEUED ->
                    targetStatus == ExecutionStatus.RUNNING;

            case RUNNING ->
                    targetStatus == ExecutionStatus.SUCCESS
                            || targetStatus == ExecutionStatus.FAILED;

            case FAILED ->
                    targetStatus == ExecutionStatus.RETRYING
                            || targetStatus == ExecutionStatus.DEAD;

            case RETRYING ->
                    targetStatus == ExecutionStatus.QUEUED;

            case SUCCESS -> false;

            case DEAD -> false;

            case CANCELLED -> false;
        };

        if (!valid) {
            throw new BusinessRuleException(
                    "Invalid execution state transition: "
                            + currentStatus
                            + " -> "
                            + targetStatus
            );
        }
    }

    private JobExecutionResponse mapToResponse(
            JobExecution execution
    ) {

        return new JobExecutionResponse(
                execution.getId(),
                execution.getIdempotencyKey(),
                execution.getJob().getId(),
                execution.getJob().getTaskType(),
                execution.getJob().getTaskPayload(),
                execution.getStatus(),
                execution.getAttemptNumber(),
                execution.getStartedAt(),
                execution.getCompletedAt(),
                execution.getErrorMessage(),
                execution.getCreatedAt()
        );
    }
}