package com.jobscheduler.job_scheduler.service.handler;

import com.jobscheduler.job_scheduler.entity.JobTaskType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmailJobHandler implements JobHandler {

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;

    @Override
    public JobTaskType getTaskType() {
        return JobTaskType.EMAIL;
    }

    @Override
    public void execute(
            String payload,
            String idempotencyKey
    ) {

        try {

            JsonNode json =
                    objectMapper.readTree(payload);

            String email =
                    json.get("email").asText();

            String subject =
                    json.get("subject").asText();

            String body =
                    json.get("body").asText();

            SimpleMailMessage message =
                    new SimpleMailMessage();

            message.setTo(email);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

            log.info(
                    "Email sent successfully. recipient={}, idempotencyKey={}",
                    email,
                    idempotencyKey
            );

        } catch (Exception exception) {

            log.error(
                    "Failed to send email. idempotencyKey={}",
                    idempotencyKey,
                    exception
            );

            throw new RuntimeException(
                    "Email sending failed",
                    exception
            );
        }
    }
}