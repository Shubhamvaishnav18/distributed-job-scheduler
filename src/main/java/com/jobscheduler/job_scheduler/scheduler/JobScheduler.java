package com.jobscheduler.job_scheduler.scheduler;

import com.jobscheduler.job_scheduler.entity.Job;
import com.jobscheduler.job_scheduler.entity.JobStatus;
import com.jobscheduler.job_scheduler.repository.JobRepository;
import com.jobscheduler.job_scheduler.service.JobDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobScheduler {

    private final JobRepository jobRepository;
    private final JobDispatchService jobDispatchService;

    @Scheduled(fixedDelay = 5000)
    public void scheduleDueJobs() {

        log.info("Scheduler checking for due jobs...");

        LocalDateTime now = LocalDateTime.now();

        List<Job> dueJobs = jobRepository
                .findDueJobs(JobStatus.SCHEDULED, now);

        for (Job job : dueJobs) {

            try {

                jobDispatchService.dispatchJob(
                        job.getId()
                );

            } catch (OptimisticLockingFailureException exception) {

                log.debug(
                        "Job {} was already claimed by another scheduler",
                        job.getId()
                );

            } catch (Exception exception) {

                log.error(
                        "Failed to dispatch job {}",
                        job.getId(),
                        exception
                );
            }
        }
    }
}