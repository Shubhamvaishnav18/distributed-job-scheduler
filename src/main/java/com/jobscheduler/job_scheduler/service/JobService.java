package com.jobscheduler.job_scheduler.service;

import com.jobscheduler.job_scheduler.dto.request.CreateJobRequest;
import com.jobscheduler.job_scheduler.dto.request.UpdateJobRequest;
import com.jobscheduler.job_scheduler.dto.response.JobResponse;
import org.springframework.data.domain.Page;

public interface JobService {

    JobResponse createJob(CreateJobRequest request);

    Page<JobResponse> getJobs(
            int page,
            int size,
            String sortBy,
            String direction,
            String status
    );

    JobResponse getJobById(Long id);

    JobResponse updateJob(
            Long id,
            UpdateJobRequest request
    );

    void deleteJob(Long id);
}