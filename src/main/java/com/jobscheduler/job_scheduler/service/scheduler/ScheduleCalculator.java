package com.jobscheduler.job_scheduler.service.scheduler;

import com.jobscheduler.job_scheduler.entity.JobScheduleType;
import com.jobscheduler.job_scheduler.exception.BusinessRuleException;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class ScheduleCalculator {

    public LocalDateTime calculateInitialNextRunAt(
            JobScheduleType scheduleType,
            LocalDateTime scheduledAt,
            String scheduleExpression
    ) {

        validate(
                scheduleType,
                scheduledAt,
                scheduleExpression
        );

        return switch (scheduleType) {

            case ONE_TIME -> scheduledAt;

            case CRON -> {
                CronExpression cron =
                        CronExpression.parse(scheduleExpression);

                yield cron.next(LocalDateTime.now());
            }

            case RECURRING -> {
                Duration duration =
                        parseRecurringDuration(scheduleExpression);

                if (scheduledAt != null) {
                    yield scheduledAt;
                }

                yield LocalDateTime.now().plus(duration);
            }
        };
    }

    public LocalDateTime calculateNextRunAt(
            JobScheduleType scheduleType,
            LocalDateTime currentRunAt,
            String scheduleExpression
    ) {

        if (scheduleType == JobScheduleType.ONE_TIME) {
            return null;
        }

        if (scheduleType == JobScheduleType.CRON) {

            CronExpression cron =
                    CronExpression.parse(scheduleExpression);

            LocalDateTime next =
                    cron.next(currentRunAt);

            if (next == null) {
                throw new BusinessRuleException(
                        "No future execution found for CRON expression"
                );
            }

            return next;
        }

        Duration duration =
                parseRecurringDuration(scheduleExpression);

        return currentRunAt.plus(duration);
    }

    private void validate(
            JobScheduleType scheduleType,
            LocalDateTime scheduledAt,
            String scheduleExpression
    ) {

        if (scheduleType == JobScheduleType.ONE_TIME) {

            if (scheduledAt == null) {
                throw new BusinessRuleException(
                        "scheduledAt is required for ONE_TIME jobs"
                );
            }

            if (scheduleExpression != null
                    && !scheduleExpression.isBlank()) {

                throw new BusinessRuleException(
                        "scheduleExpression must be null for ONE_TIME jobs"
                );
            }
        }

        if (scheduleType == JobScheduleType.CRON) {

            if (scheduleExpression == null
                    || scheduleExpression.isBlank()) {

                throw new BusinessRuleException(
                        "scheduleExpression is required for CRON jobs"
                );
            }

            try {
                CronExpression.parse(scheduleExpression);
            } catch (IllegalArgumentException exception) {
                throw new BusinessRuleException(
                        "Invalid CRON expression: "
                                + scheduleExpression
                );
            }

            if (scheduledAt != null) {
                throw new BusinessRuleException(
                        "scheduledAt must be null for CRON jobs"
                );
            }
        }

        if (scheduleType == JobScheduleType.RECURRING) {

            if (scheduledAt == null) {
                throw new BusinessRuleException(
                        "scheduledAt is required for RECURRING jobs"
                );
            }

            if (scheduleExpression == null
                    || scheduleExpression.isBlank()) {

                throw new BusinessRuleException(
                        "scheduleExpression is required for RECURRING jobs"
                );
            }

            parseRecurringDuration(scheduleExpression);
        }
    }

    private Duration parseRecurringDuration(
            String scheduleExpression
    ) {

        try {

            Duration duration =
                    Duration.parse(scheduleExpression);

            if (duration.isZero()
                    || duration.isNegative()) {

                throw new BusinessRuleException(
                        "Recurring duration must be greater than zero"
                );
            }

            return duration;

        } catch (BusinessRuleException exception) {
            throw exception;

        } catch (Exception exception) {

            throw new BusinessRuleException(
                    "Invalid RECURRING duration. "
                            + "Use ISO-8601 format like PT5M, PT1H or P1D"
            );
        }
    }
}