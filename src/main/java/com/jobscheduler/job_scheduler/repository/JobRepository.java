package com.jobscheduler.job_scheduler.repository;

import com.jobscheduler.job_scheduler.entity.Job;
import com.jobscheduler.job_scheduler.entity.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    Page<Job> findByUserId(Long userId, Pageable pageable);

    Page<Job> findByStatus(JobStatus status, Pageable pageable);

    Page<Job> findByUserIdAndStatus(
            Long userId,
            JobStatus status,
            Pageable pageable
    );

    @Query("""
        SELECT j
        FROM Job j
        WHERE j.status = :status
        AND j.nextRunAt IS NOT NULL
        AND j.nextRunAt <= :currentTime
        """)
    List<Job> findDueJobs(
            @Param("status") JobStatus status,
            @Param("currentTime") LocalDateTime currentTime
    );
}