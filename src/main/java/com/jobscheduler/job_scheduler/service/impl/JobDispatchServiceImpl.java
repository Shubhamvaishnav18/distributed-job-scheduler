package com.jobscheduler.job_scheduler.service.impl;

import com.jobscheduler.job_scheduler.dto.response.JobExecutionResponse;
import com.jobscheduler.job_scheduler.entity.Job;
import com.jobscheduler.job_scheduler.entity.JobStatus;
import com.jobscheduler.job_scheduler.entity.OutboxEvent;
import com.jobscheduler.job_scheduler.entity.OutboxStatus;
import com.jobscheduler.job_scheduler.exception.ResourceNotFoundException;
import com.jobscheduler.job_scheduler.repository.JobRepository;
import com.jobscheduler.job_scheduler.repository.OutboxRepository;
import com.jobscheduler.job_scheduler.service.JobDispatchService;
import com.jobscheduler.job_scheduler.service.JobExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobDispatchServiceImpl implements JobDispatchService {

    private final JobRepository jobRepository;
    private final JobExecutionService jobExecutionService;
    private final OutboxRepository outboxRepository;

    @Override
    @Transactional
    public void dispatchJob(Long jobId) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job with id " + jobId + " not found"
                        )
                );

        if (job.getStatus() != JobStatus.SCHEDULED) {
            return;
        }

        job.setStatus(JobStatus.DISPATCHED);

        jobRepository.saveAndFlush(job);

        JobExecutionResponse execution =
                jobExecutionService.createExecution(jobId);

        // Outbox event
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .eventType("JOB_EXECUTION_CREATED")
                .aggregateId(execution.id())
                .payload(
                        "{\"executionId\":" + execution.id() + "}"
                )
                .status(OutboxStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .build();

        outboxRepository.save(outboxEvent);

        log.info(
                "Job {} dispatched and execution {} queued",
                jobId,
                execution.id()
        );
    }
}