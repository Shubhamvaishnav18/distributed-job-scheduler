package com.jobscheduler.job_scheduler.service;

import com.jobscheduler.job_scheduler.entity.JobPriority;

public interface JobQueueService {

    void enqueue(Long executionId, JobPriority priority);

    Long dequeue();
}