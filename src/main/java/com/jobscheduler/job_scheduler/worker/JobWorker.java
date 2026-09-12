package com.jobscheduler.job_scheduler.worker;

import com.jobscheduler.job_scheduler.dto.response.JobExecutionResponse;
import com.jobscheduler.job_scheduler.entity.ExecutionStatus;
import com.jobscheduler.job_scheduler.entity.JobTaskType;
import com.jobscheduler.job_scheduler.exception.BusinessRuleException;
import com.jobscheduler.job_scheduler.service.JobExecutionService;
import com.jobscheduler.job_scheduler.service.JobMetricsService;
import com.jobscheduler.job_scheduler.service.JobQueueService;
import com.jobscheduler.job_scheduler.service.handler.JobHandler;
import com.jobscheduler.job_scheduler.service.handler.JobHandlerRegistry;
import com.jobscheduler.job_scheduler.service.idempotency.IdempotencyService;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobWorker {

    private final JobQueueService jobQueueService;
    private final JobExecutionService jobExecutionService;
    private final JobHandlerRegistry jobHandlerRegistry;
    private final IdempotencyService idempotencyService;
    private final JobMetricsService jobMetricsService;
    private final MeterRegistry meterRegistry;

    @Value("${worker.instance-id:local-worker}")
    private String workerInstanceId;

    @Scheduled(fixedDelay = 1000)
    public void processQueue() {

        Long executionId =
                jobQueueService.dequeue();

        if (executionId == null) {
            return;
        }

        processExecution(executionId);
    }

    private void processExecution(Long executionId) {

        long startTime = System.currentTimeMillis();
        Timer.Sample sample =
                Timer.start(meterRegistry);

        try {

            log.info(
                    "Worker {} processing execution {}",
                    workerInstanceId,
                    executionId
            );

            // 1. Get execution
            JobExecutionResponse execution =
                    jobExecutionService.getExecution(
                            executionId
                    );

            // 2. Cancellation check
            if (execution.status() == ExecutionStatus.CANCELLED) {

                log.info(
                        "Worker {}: Execution {} is cancelled. Skipping.",
                        workerInstanceId,
                        executionId
                );

                return;
            }

            try {
                execution =
                        jobExecutionService.claimExecution(
                                executionId
                        );

                long queueLagMillis =
                        System.currentTimeMillis()
                                - execution.createdAt()
                                .atZone(java.time.ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli();

                jobMetricsService.recordQueueLag(queueLagMillis);
            } catch (BusinessRuleException exception) {

                log.warn(
                        "Execution {} could not be claimed. Skipping.",
                        executionId
                );

                return;
            }

            String processingIdempotencyKey =
                    execution.id()
                            + "-attempt-"
                            + execution.attemptNumber();

            boolean claimed =
                    idempotencyService.claim(
                            processingIdempotencyKey
                    );

            if (!claimed) {

                log.info(
                        "Worker {}: Execution {} attempt {} is already processed. Skipping.",
                        workerInstanceId,
                        executionId,
                        execution.attemptNumber()
                );

                return;
            }

            JobTaskType taskType =
                    getTaskType(execution);

            JobHandler handler =
                    jobHandlerRegistry.getHandler(taskType);

            handler.execute(
                    execution.taskPayload(),
                    execution.idempotencyKey()
            );

            JobExecutionResponse completedExecution =
                    jobExecutionService.completeExecution(
                            executionId
                    );

            jobMetricsService.recordSuccess(
                    execution.taskType().name(),
                    workerInstanceId
            );

            log.info(
                    "Execution {} completed successfully",
                    executionId
            );

        } catch (Exception exception) {

            log.error(
                    "Execution {} failed: {}",
                    executionId,
                    exception.getMessage(),
                    exception
            );

            try {

                JobExecutionResponse result =
                        jobExecutionService.retryOrFail(
                                executionId,
                                exception.getMessage()
                        );

                log.info(
                        "Retry decision for execution {}: {}",
                        executionId,
                        result.status()
                );

            } catch (Exception retryException) {

                log.error(
                        "Failed to process retry for execution {}",
                        executionId,
                        retryException
                );
            }
        } finally {

            long duration =
                    System.currentTimeMillis() - startTime;

            sample.stop(
                    Timer.builder("job.execution.duration")
                            .description("Job execution duration")
                            .register(meterRegistry)
            );

            log.info(
                    "Execution {} processed by {} in {} ms",
                    executionId,
                    workerInstanceId,
                    duration
            );
        }
    }

    private JobTaskType getTaskType(
            JobExecutionResponse execution
    ) {
        return execution.taskType();
    }
}