package com.jobscheduler.job_scheduler.publisher;

import com.jobscheduler.job_scheduler.entity.JobExecution;
import com.jobscheduler.job_scheduler.entity.JobPriority;
import com.jobscheduler.job_scheduler.entity.OutboxEvent;
import com.jobscheduler.job_scheduler.entity.OutboxStatus;
import com.jobscheduler.job_scheduler.exception.ResourceNotFoundException;
import com.jobscheduler.job_scheduler.repository.JobExecutionRepository;
import com.jobscheduler.job_scheduler.repository.OutboxRepository;
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
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final JobQueueService jobQueueService;
    private final JobExecutionRepository jobExecutionRepository;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPendingEvents() {

        List<OutboxEvent> events =
                outboxRepository.findByStatus(
                        OutboxStatus.PENDING
                );

        for (OutboxEvent event : events) {

            publishEvent(event);
        }
    }

    @Transactional
    public void publishEvent(OutboxEvent event) {

        try {

            JobExecution execution =
                    jobExecutionRepository.findById(
                                    event.getAggregateId()
                            )
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Execution not found: "
                                                    + event.getAggregateId()
                                    )
                            );

            JobPriority priority =
                    execution.getJob().getPriority();

            jobQueueService.enqueue(
                    execution.getId(),
                    priority
            );

            event.setStatus(
                    OutboxStatus.PUBLISHED
            );

            event.setPublishedAt(
                    LocalDateTime.now()
            );

            outboxRepository.save(event);

            log.info(
                    "Outbox event {} published successfully. Execution: {}",
                    event.getId(),
                    event.getAggregateId()
            );

        } catch (Exception exception) {

            int retryCount =
                    event.getRetryCount() == null
                            ? 0
                            : event.getRetryCount();

            event.setRetryCount(
                    retryCount + 1
            );

            outboxRepository.save(event);

            log.error(
                    "Failed to publish outbox event {}. Retry count: {}",
                    event.getId(),
                    event.getRetryCount(),
                    exception
            );
        }
    }
}