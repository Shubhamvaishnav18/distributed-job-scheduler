package com.jobscheduler.job_scheduler.service.handler;

import com.jobscheduler.job_scheduler.entity.JobTaskType;
import com.jobscheduler.job_scheduler.entity.Notification;
import com.jobscheduler.job_scheduler.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationJobHandler implements JobHandler {

    private final ObjectMapper objectMapper;
    private final NotificationRepository notificationRepository;

    @Override
    public JobTaskType getTaskType() {
        return JobTaskType.NOTIFICATION;
    }

    @Override
    public void execute(
            String payload,
            String idempotencyKey
    ) {

        try {

            JsonNode root =
                    objectMapper.readTree(payload);

            String channel =
                    root.path("channel").asText(null);

            String title =
                    root.path("title").asText(null);

            String message =
                    root.path("message").asText(null);

            String recipient =
                    root.path("recipient").asText(null);

            if (channel == null || channel.isBlank()) {
                throw new IllegalArgumentException(
                        "Notification channel is required"
                );
            }

            if (!channel.equalsIgnoreCase("IN_APP")) {
                throw new IllegalArgumentException(
                        "Notification channel must be IN_APP"
                );
            }

            if (title == null || title.isBlank()) {
                throw new IllegalArgumentException(
                        "Notification title is required"
                );
            }

            if (message == null || message.isBlank()) {
                throw new IllegalArgumentException(
                        "Notification message is required"
                );
            }

            if (recipient == null || recipient.isBlank()) {
                throw new IllegalArgumentException(
                        "Notification recipient is required"
                );
            }

            Notification notification =
                    Notification.builder()
                            .channel(channel)
                            .title(title)
                            .message(message)
                            .recipient(recipient)
                            .readStatus(false)
                            .idempotencyKey(idempotencyKey)
                            .build();

            Notification saved =
                    notificationRepository.save(notification);

            log.info(
                    "Notification created successfully. id={}, recipient={}, idempotencyKey={}",
                    saved.getId(),
                    saved.getRecipient(),
                    idempotencyKey
            );

        } catch (Exception exception) {

            throw new RuntimeException(
                    "Invalid notification payload",
                    exception
            );
        }
    }
}