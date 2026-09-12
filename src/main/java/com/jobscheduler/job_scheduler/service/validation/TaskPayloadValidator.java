package com.jobscheduler.job_scheduler.service.validation;

import tools.jackson.databind.JsonNode;
import com.jobscheduler.job_scheduler.entity.JobTaskType;
import com.jobscheduler.job_scheduler.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

@Component
public class TaskPayloadValidator {

    public void validate(
            JobTaskType taskType,
            JsonNode payload
    ) {

        if (payload == null || payload.isNull()) {
            throw new BusinessRuleException(
                    "Task payload is required"
            );
        }

        if (!payload.isObject()) {
            throw new BusinessRuleException(
                    "Task payload must be a JSON object"
            );
        }

        switch (taskType) {

            case EMAIL -> validateEmail(payload);

            case WEBHOOK -> validateWebhook(payload);

            case REPORT -> validateReport(payload);

            case NOTIFICATION -> validateNotification(payload);
        }
    }

    private void validateEmail(JsonNode payload) {

        requireText(
                payload,
                "email",
                "Email recipient is required"
        );

        requireText(
                payload,
                "subject",
                "Email subject is required"
        );

        requireText(
                payload,
                "body",
                "Email body is required"
        );

        String email =
                payload.get("email").asText();

        if (!email.matches(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
        )) {
            throw new BusinessRuleException(
                    "Invalid email address"
            );
        }
    }

    private void validateWebhook(JsonNode payload) {

        requireText(
                payload,
                "url",
                "Webhook URL is required"
        );

        String url =
                payload.get("url").asText();

        if (!url.startsWith("http://")
                && !url.startsWith("https://")) {

            throw new BusinessRuleException(
                    "Webhook URL must start with http:// or https://"
            );
        }

        if (payload.has("method")
                && !payload.get("method").isTextual()) {

            throw new BusinessRuleException(
                    "Webhook method must be a string"
            );
        }

        if (payload.has("headers")
                && !payload.get("headers").isObject()) {

            throw new BusinessRuleException(
                    "Webhook headers must be a JSON object"
            );
        }
    }

    private void validateReport(JsonNode payload) {

        requireText(
                payload,
                "reportType",
                "Report type is required"
        );

        requireText(
                payload,
                "format",
                "Report format is required"
        );

        String format =
                payload.get("format")
                        .asText()
                        .toUpperCase();

        if (!format.equals("CSV")
                && !format.equals("JSON")) {

            throw new BusinessRuleException(
                    "Report format must be CSV or JSON"
            );
        }
    }

    private void validateNotification(JsonNode payload) {

        requireText(
                payload,
                "channel",
                "Notification channel is required"
        );

        requireText(
                payload,
                "recipient",
                "Notification recipient is required"
        );

        requireText(
                payload,
                "title",
                "Notification title is required"
        );

        requireText(
                payload,
                "message",
                "Notification message is required"
        );

        String channel =
                payload.get("channel")
                        .asText()
                        .toUpperCase();

        if (!channel.equals("IN_APP")) {

            throw new BusinessRuleException(
                    "Notification channel must be IN_APP"
            );
        }
    }

    private void requireText(
            JsonNode payload,
            String field,
            String message
    ) {

        if (!payload.has(field)
                || payload.get(field).isNull()
                || !payload.get(field).isTextual()
                || payload.get(field).asText().isBlank()) {

            throw new BusinessRuleException(message);
        }
    }
}