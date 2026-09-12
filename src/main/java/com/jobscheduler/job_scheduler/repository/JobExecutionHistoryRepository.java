package com.jobscheduler.job_scheduler.repository;

import com.jobscheduler.job_scheduler.entity.JobExecutionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobExecutionHistoryRepository
        extends JpaRepository<JobExecutionHistory, Long> {

    List<JobExecutionHistory> findByExecutionIdOrderByCreatedAtAsc(
            Long executionId
    );
}
