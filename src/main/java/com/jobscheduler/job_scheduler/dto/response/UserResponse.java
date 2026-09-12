package com.jobscheduler.job_scheduler.dto.response;

public record UserResponse(
        Long id,
        String name,
        String email
) {
}