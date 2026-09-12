package com.jobscheduler.job_scheduler.repository;

import com.jobscheduler.job_scheduler.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {
}