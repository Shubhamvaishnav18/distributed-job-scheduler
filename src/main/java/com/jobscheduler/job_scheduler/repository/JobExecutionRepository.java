package com.jobscheduler.job_scheduler.repository;

import com.jobscheduler.job_scheduler.entity.ExecutionStatus;
import com.jobscheduler.job_scheduler.entity.JobExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JobExecutionRepository
        extends JpaRepository<JobExecution, Long> {

    List<JobExecution> findByJobId(Long jobId);

    List<JobExecution> findByJobIdOrderByCreatedAtDesc(
            Long jobId
    );

    List<JobExecution> findByStatus(ExecutionStatus status);

    Optional<JobExecution> findByIdempotencyKey(String idempotencyKey);

    @Query("""
        SELECT e
        FROM JobExecution e
        JOIN FETCH e.job
        WHERE e.id = :executionId
        """)
    Optional<JobExecution> findByIdWithJob(
            @Param("executionId") Long executionId
    );

    @Query("""
        SELECT e
        FROM JobExecution e
        WHERE e.status = :status
        AND e.nextRetryAt IS NOT NULL
        AND e.nextRetryAt <= :currentTime
        """)
    List<JobExecution> findDueRetries(
            @Param("status") ExecutionStatus status,
            @Param("currentTime") LocalDateTime currentTime
    );

    @Modifying
    @Query("""
        UPDATE JobExecution e
        SET e.status = 'RUNNING',
            e.startedAt = CURRENT_TIMESTAMP
        WHERE e.id = :executionId
        AND e.status = 'QUEUED'
        """)
    int claimExecution(
            @Param("executionId") Long executionId
    );

    @Modifying
    @Query("""
    UPDATE JobExecution e
    SET e.status = 'CANCELLED',
        e.completedAt = CURRENT_TIMESTAMP,
        e.nextRetryAt = NULL
    WHERE e.id = :executionId
    AND e.status IN ('QUEUED', 'RETRYING')
    """)
    int cancelExecution(
            @Param("executionId") Long executionId
    );

    @Modifying
    @Query("""
    UPDATE JobExecution e
    SET e.status = 'SUCCESS',
        e.completedAt = CURRENT_TIMESTAMP
    WHERE e.id = :executionId
    AND e.status = 'RUNNING'
    """)
    int completeExecution(
            @Param("executionId") Long executionId
    );
}