package com.jobscheduler.job_scheduler.scheduler;

import com.jobscheduler.job_scheduler.entity.ExecutionStatus;
import com.jobscheduler.job_scheduler.entity.JobExecution;
import com.jobscheduler.job_scheduler.repository.JobExecutionRepository;
import com.jobscheduler.job_scheduler.service.JobExecutionHistoryService;
import com.jobscheduler.job_scheduler.service.JobQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RetryScheduler {

    private final JobExecutionRepository jobExecutionRepository;
    private final JobQueueService jobQueueService;
    private final JobExecutionHistoryService jobExecutionHistoryService;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void processRetries() {

        LocalDateTime now =
                LocalDateTime.now();

        List<JobExecution> dueRetries =
                jobExecutionRepository.findDueRetries(
                        ExecutionStatus.RETRYING,
                        now
                );

        for (JobExecution execution : dueRetries) {

            execution.setStatus(
                    ExecutionStatus.QUEUED
            );

            execution.setAttemptNumber(
                    execution.getAttemptNumber() + 1
            );

            execution.setNextRetryAt(null);

            jobExecutionRepository.save(execution);

            jobExecutionHistoryService.record(
                    execution,
                    ExecutionStatus.QUEUED,
                    null
            );

            jobQueueService.enqueue(
                    execution.getId(),
                    execution.getJob().getPriority()
            );

            log.info(
                    "Retry queued for execution {}. Attempt: {}",
                    execution.getId(),
                    execution.getAttemptNumber()
            );
        }
    }
}