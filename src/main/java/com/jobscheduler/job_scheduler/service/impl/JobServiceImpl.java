package com.jobscheduler.job_scheduler.service.impl;

import com.jobscheduler.job_scheduler.dto.request.CreateJobRequest;
import com.jobscheduler.job_scheduler.dto.request.UpdateJobRequest;
import com.jobscheduler.job_scheduler.dto.response.JobResponse;
import com.jobscheduler.job_scheduler.entity.*;
import com.jobscheduler.job_scheduler.exception.BusinessRuleException;
import com.jobscheduler.job_scheduler.exception.ResourceNotFoundException;
import com.jobscheduler.job_scheduler.repository.JobRepository;
import com.jobscheduler.job_scheduler.security.CurrentUserService;
import com.jobscheduler.job_scheduler.service.JobService;
import com.jobscheduler.job_scheduler.service.scheduler.ScheduleCalculator;
import com.jobscheduler.job_scheduler.service.validation.TaskPayloadValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final CurrentUserService currentUserService;
    private final ScheduleCalculator scheduleCalculator;
    private final TaskPayloadValidator taskPayloadValidator;

    private JobResponse mapToJobResponse(Job job) {

        return new JobResponse(
                job.getId(),
                job.getName(),
                job.getDescription(),
                job.getScheduleType(),
                job.getTaskType(),
                job.getTaskPayload(),
                job.getStatus(),
                job.getScheduleExpression(),
                job.getMaxRetries(),
                job.getPriority(),
                job.getNextRunAt(),
                job.getUser().getId(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }

    private void validateJobCanBeModified(Job job) {

        if (job.getStatus() == JobStatus.RUNNING) {

            throw new BusinessRuleException(
                    "Running job cannot be modified"
            );
        }

        if (job.getStatus() == JobStatus.COMPLETED) {

            throw new BusinessRuleException(
                    "Completed job cannot be modified"
            );
        }

        if (job.getStatus() == JobStatus.FAILED) {

            throw new BusinessRuleException(
                    "Failed job cannot be modified"
            );
        }
    }

    private void validateJobCanBeDeleted(Job job) {

        if (job.getStatus() == JobStatus.RUNNING) {

            throw new BusinessRuleException(
                    "Running job cannot be deleted"
            );
        }

        if (job.getStatus() == JobStatus.COMPLETED) {

            throw new BusinessRuleException(
                    "Completed job cannot be deleted"
            );
        }
    }

    @Override
    public JobResponse createJob(CreateJobRequest request) {

        User currentUser =
                currentUserService.getCurrentUser();

        taskPayloadValidator.validate(
                request.taskType(),
                request.taskPayload()
        );

        LocalDateTime nextRunAt =
                scheduleCalculator.calculateInitialNextRunAt(
                        request.scheduleType(),
                        request.scheduledAt(),
                        request.scheduleExpression()
                );

        JobPriority priority =
                request.priority() != null
                        ? request.priority()
                        : JobPriority.MEDIUM;

        Job job = Job.builder()
                .name(request.name())
                .description(request.description())
                .scheduleType(request.scheduleType())
                .taskType(request.taskType())
                .taskPayload(request.taskPayload().toString())
                .status(JobStatus.SCHEDULED)
                .scheduledAt(request.scheduledAt())
                .scheduleExpression(request.scheduleExpression())
                .nextRunAt(nextRunAt)
                .maxRetries(request.maxRetries())
                .priority(priority)
                .user(currentUser)
                .build();

        Job savedJob = jobRepository.save(job);

        return mapToJobResponse(savedJob);
    }

    @Override
    public Page<JobResponse> getJobs(
            int page,
            int size,
            String sortBy,
            String direction,
            String status
    ) {

        User currentUser =
                currentUserService.getCurrentUser();

        Sort.Direction sortDirection =
                direction.equalsIgnoreCase("desc")
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, sortBy)
        );

        Page<Job> jobs;

        JobStatus jobStatus = null;

        if (status != null && !status.isBlank()) {
            try {
                jobStatus = JobStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException exception) {
                throw new BusinessRuleException(
                        "Invalid job status: " + status
                );
            }
        }

        if (jobStatus != null) {

            jobs =
                    jobRepository.findByUserIdAndStatus(
                            currentUser.getId(),
                            jobStatus,
                            pageable
                    );

        } else {

            jobs =
                    jobRepository.findByUserId(
                            currentUser.getId(),
                            pageable
                    );
        }

        return jobs.map(this::mapToJobResponse);
    }

    @Override
    public JobResponse getJobById(Long id) {

        Job job = jobRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job with id " + id + " not found"
                        )
                );

        User currentUser =
                currentUserService.getCurrentUser();

        if (!job.getUser().getId()
                .equals(currentUser.getId())) {

            throw new BusinessRuleException(
                    "You do not have access to this job"
            );
        }

        return mapToJobResponse(job);
    }

    @Override
    public JobResponse updateJob(
            Long id,
            UpdateJobRequest request
    ) {

        Job job = jobRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job with id " + id + " not found"
                        )
                );

        User currentUser =
                currentUserService.getCurrentUser();

        if (!job.getUser().getId()
                .equals(currentUser.getId())) {

            throw new BusinessRuleException(
                    "You do not have access to this job"
            );
        }

        validateJobCanBeModified(job);

        taskPayloadValidator.validate(
                request.taskType(),
                request.taskPayload()
        );

        LocalDateTime nextRunAt =
                scheduleCalculator.calculateInitialNextRunAt(
                        request.scheduleType(),
                        request.scheduledAt(),
                        request.scheduleExpression()
                );

        JobPriority priority =
                request.priority() != null
                        ? request.priority()
                        : JobPriority.MEDIUM;

        job.setName(request.name());
        job.setDescription(request.description());
        job.setScheduleType(request.scheduleType());
        job.setTaskType(request.taskType());
        job.setTaskPayload(request.taskPayload().toString());
        job.setScheduledAt(request.scheduledAt());
        job.setScheduleExpression(request.scheduleExpression());
        job.setNextRunAt(nextRunAt);
        job.setMaxRetries(request.maxRetries());
        job.setPriority(priority);

        Job updatedJob = jobRepository.save(job);

        return mapToJobResponse(updatedJob);
    }

    @Override
    public void deleteJob(Long id) {

        Job job = jobRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job with id " + id + " not found"
                        )
                );

        User currentUser =
                currentUserService.getCurrentUser();

        if (!job.getUser().getId()
                .equals(currentUser.getId())) {

            throw new BusinessRuleException(
                    "You do not have access to this job"
            );
        }

        validateJobCanBeDeleted(job);

        jobRepository.delete(job);
    }
}