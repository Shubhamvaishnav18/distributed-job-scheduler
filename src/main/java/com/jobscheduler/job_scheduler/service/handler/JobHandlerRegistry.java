package com.jobscheduler.job_scheduler.service.handler;

import com.jobscheduler.job_scheduler.entity.JobTaskType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class JobHandlerRegistry {

    private final Map<JobTaskType, JobHandler> handlers;

    public JobHandlerRegistry(List<JobHandler> jobHandlers) {

        this.handlers = new EnumMap<>(JobTaskType.class);

        for (JobHandler handler : jobHandlers) {
            handlers.put(handler.getTaskType(), handler);
        }
    }

    public JobHandler getHandler(JobTaskType taskType) {

        JobHandler handler = handlers.get(taskType);

        if (handler == null) {
            throw new IllegalArgumentException(
                    "No handler found for task type: " + taskType
            );
        }

        return handler;
    }
}