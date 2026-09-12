package com.jobscheduler.job_scheduler.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "job.retry")
public class RetryProperties {

    private long baseDelaySeconds = 5;
}