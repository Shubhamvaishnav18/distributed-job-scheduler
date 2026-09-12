package com.jobscheduler.job_scheduler.service.retry;

import com.jobscheduler.job_scheduler.config.RetryProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RetryPolicy {

    private final RetryProperties retryProperties;

    public long getDelaySeconds(int attemptNumber) {

        return retryProperties.getBaseDelaySeconds()
                * (1L << (attemptNumber - 1));
    }
}