package com.jobscheduler.job_scheduler.service.handler;

import com.jobscheduler.job_scheduler.entity.JobTaskType;

public interface JobHandler {

    JobTaskType getTaskType();

    void execute(String payload, String idempotencyKey);
}