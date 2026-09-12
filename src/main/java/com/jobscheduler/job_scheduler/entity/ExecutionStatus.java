package com.jobscheduler.job_scheduler.entity;

public enum ExecutionStatus {
    QUEUED,
    RUNNING,
    SUCCESS,
    FAILED,
    RETRYING,
    DEAD,
    CANCELLED
}