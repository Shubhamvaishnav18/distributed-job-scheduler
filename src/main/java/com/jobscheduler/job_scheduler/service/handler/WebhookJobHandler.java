package com.jobscheduler.job_scheduler.service.handler;

import com.jobscheduler.job_scheduler.entity.JobTaskType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebhookJobHandler implements JobHandler {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Override
    public JobTaskType getTaskType() {
        return JobTaskType.WEBHOOK;
    }

    @Override
    public void execute(
            String payload,
            String idempotencyKey
    ) {

        try {

            JsonNode json =
                    objectMapper.readTree(payload);

            String url =
                    json.get("url").asText();

            String methodName =
                    json.has("method")
                            ? json.get("method").asText().toUpperCase()
                            : "POST";

            HttpMethod method =
                    HttpMethod.valueOf(methodName);

            JsonNode body =
                    json.get("body");

            RestClient.RequestBodySpec request =
                    restClient
                            .method(method)
                            .uri(url);

            if (json.has("headers")
                    && json.get("headers").isObject()) {

                json.get("headers")
                        .properties()
                        .forEach(entry ->
                                request.header(
                                        entry.getKey(),
                                        entry.getValue().asText()
                                )
                        );
            }

            if (body != null && !body.isNull()) {

                request
                        .body(
                                objectMapper.writeValueAsString(body)
                        );
            }

            request.retrieve().toBodilessEntity();

            log.info(
                    "Webhook executed successfully. url={}, method={}, idempotencyKey={}",
                    url,
                    method,
                    idempotencyKey
            );

        } catch (Exception exception) {

            log.error(
                    "Webhook execution failed. idempotencyKey={}",
                    idempotencyKey,
                    exception
            );

            throw new RuntimeException(
                    "Webhook execution failed",
                    exception
            );
        }
    }
}