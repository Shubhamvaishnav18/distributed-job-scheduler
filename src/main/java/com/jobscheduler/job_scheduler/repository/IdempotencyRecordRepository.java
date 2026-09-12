package com.jobscheduler.job_scheduler.repository;

import com.jobscheduler.job_scheduler.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IdempotencyRecordRepository
        extends JpaRepository<IdempotencyRecord, Long> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<IdempotencyRecord> findByIdempotencyKey(
            String idempotencyKey
    );

    @Modifying
    @Query(
            value = """
        INSERT IGNORE INTO idempotency_records
        (idempotency_key, processed_at)
        VALUES (:idempotencyKey, CURRENT_TIMESTAMP)
        """,
            nativeQuery = true
    )
    int claimIdempotency(
            @Param("idempotencyKey") String idempotencyKey
    );
}
