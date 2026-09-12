package com.jobscheduler.job_scheduler.controller;

import com.jobscheduler.job_scheduler.dto.response.JobExecutionHistoryResponse;
import com.jobscheduler.job_scheduler.dto.response.JobExecutionResponse;
import com.jobscheduler.job_scheduler.service.JobExecutionHistoryService;
import com.jobscheduler.job_scheduler.service.JobExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/executions")
@RequiredArgsConstructor
public class JobExecutionController {

    private final JobExecutionService jobExecutionService;
    private final JobExecutionHistoryService jobExecutionHistoryService;

    @PostMapping("/jobs/{jobId}")
    public ResponseEntity<JobExecutionResponse> createExecution(
            @PathVariable Long jobId
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(jobExecutionService.createExecution(jobId));
    }

    @PostMapping("/{executionId}/start")
    public ResponseEntity<JobExecutionResponse> startExecution(
            @PathVariable Long executionId
    ) {

        return ResponseEntity.ok(
                jobExecutionService.startExecution(executionId)
        );
    }

    @PostMapping("/{executionId}/complete")
    public ResponseEntity<JobExecutionResponse> completeExecution(
            @PathVariable Long executionId
    ) {

        return ResponseEntity.ok(
                jobExecutionService.completeExecution(executionId)
        );
    }

    @PostMapping("/{executionId}/fail")
    public ResponseEntity<JobExecutionResponse> failExecution(
            @PathVariable Long executionId,
            @RequestParam String errorMessage
    ) {

        return ResponseEntity.ok(
                jobExecutionService.failExecution(
                        executionId,
                        errorMessage
                )
        );
    }

    @GetMapping("/dead")
    public ResponseEntity<List<JobExecutionResponse>> getDeadExecutions() {

        return ResponseEntity.ok(
                jobExecutionService.getDeadExecutions()
        );
    }

    @PostMapping("/{executionId}/retry")
    public ResponseEntity<JobExecutionResponse> retryDeadExecution(
            @PathVariable Long executionId
    ) {

        return ResponseEntity.ok(
                jobExecutionService.retryDeadExecution(executionId)
        );
    }

    @GetMapping("/{executionId}/history")
    public ResponseEntity<List<JobExecutionHistoryResponse>>
    getExecutionHistory(
            @PathVariable Long executionId
    ) {

        return ResponseEntity.ok(
                jobExecutionHistoryService
                        .getHistory(executionId)
        );
    }

    @GetMapping("/jobs/{jobId}/executions")
    public ResponseEntity<List<JobExecutionResponse>>
    getJobExecutions(
            @PathVariable Long jobId
    ) {

        return ResponseEntity.ok(
                jobExecutionService
                        .getExecutions(jobId)
        );
    }

    @PostMapping("/{executionId}/cancel")
    public ResponseEntity<JobExecutionResponse> cancelExecution(
            @PathVariable Long executionId
    ) {

        return ResponseEntity.ok(
                jobExecutionService.cancelExecution(
                        executionId
                )
        );
    }
}