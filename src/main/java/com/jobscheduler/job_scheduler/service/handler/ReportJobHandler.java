package com.jobscheduler.job_scheduler.service.handler;

import com.jobscheduler.job_scheduler.entity.JobTaskType;
import com.jobscheduler.job_scheduler.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReportJobHandler implements JobHandler {

    private final ObjectMapper objectMapper;

    @Override
    public JobTaskType getTaskType() {
        return JobTaskType.REPORT;
    }

    @Override
    public void execute(
            String payload,
            String idempotencyKey
    ) {

        try {

            JsonNode root =
                    objectMapper.readTree(payload);

            validateReport(root);

            String reportType =
                    root.get("reportType")
                            .asText()
                            .toUpperCase();

            String format =
                    root.get("format")
                            .asText()
                            .toUpperCase();

            String reportName =
                    root.path("reportName")
                            .asText("job-report");

            JsonNode columnsNode =
                    root.path("columns");

            JsonNode rowsNode =
                    root.path("rows");

            if (!columnsNode.isArray()
                    || columnsNode.isEmpty()) {

                throw new BusinessRuleException(
                        "Report payload must contain a non-empty 'columns' array"
                );
            }

            if (!rowsNode.isArray()) {

                throw new BusinessRuleException(
                        "Report payload must contain a 'rows' array"
                );
            }

            List<String> columns =
                    new ArrayList<>();

            for (JsonNode column : columnsNode) {

                columns.add(
                        column.asText()
                );
            }

            Path reportsDirectory =
                    Paths.get("reports");

            Files.createDirectories(
                    reportsDirectory
            );

            String timestamp =
                    LocalDateTime.now()
                            .format(
                                    DateTimeFormatter.ofPattern(
                                            "yyyyMMdd_HHmmss"
                                    )
                            );

            String safeReportName =
                    reportName.replaceAll(
                            "[^a-zA-Z0-9-_]",
                            "_"
                    );

            String safeIdempotencyKey =
                    idempotencyKey.replaceAll(
                            "[^a-zA-Z0-9-_]",
                            "_"
                    );

            String fileExtension =
                    format.equals("CSV")
                            ? ".csv"
                            : ".json";

            Path reportFile =
                    reportsDirectory.resolve(
                            safeReportName
                                    + "_"
                                    + timestamp
                                    + "_"
                                    + safeIdempotencyKey
                                    + fileExtension
                    );

            if (format.equals("CSV")) {

                generateCsvReport(
                        reportFile,
                        columns,
                        rowsNode
                );

            } else {

                generateJsonReport(
                        reportFile,
                        rowsNode
                );
            }

            log.info(
                    "Report generated successfully. type={}, format={}, file={}, rows={}, idempotencyKey={}",
                    reportType,
                    format,
                    reportFile.toAbsolutePath(),
                    rowsNode.size(),
                    idempotencyKey
            );

        } catch (BusinessRuleException exception) {

            throw exception;

        } catch (IOException exception) {

            throw new RuntimeException(
                    "Failed to generate report",
                    exception
            );

        } catch (Exception exception) {

            throw new RuntimeException(
                    "Invalid report payload",
                    exception
            );
        }
    }

    private void validateReport(
            JsonNode payload
    ) {

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

    private void requireText(
            JsonNode payload,
            String field,
            String message
    ) {

        if (!payload.has(field)
                || payload.get(field).isNull()
                || !payload.get(field).isTextual()
                || payload.get(field).asText().isBlank()) {

            throw new BusinessRuleException(
                    message
            );
        }
    }

    private void generateCsvReport(
            Path reportFile,
            List<String> columns,
            JsonNode rowsNode
    ) throws IOException {

        try (BufferedWriter writer =
                     Files.newBufferedWriter(
                             reportFile,
                             StandardOpenOption.CREATE_NEW
                     )) {

            writer.write(
                    toCsvRow(columns)
            );

            writer.newLine();

            for (JsonNode row : rowsNode) {

                List<String> values =
                        new ArrayList<>();

                for (String column : columns) {

                    JsonNode value =
                            row.get(column);

                    values.add(
                            value == null
                                    || value.isNull()
                                    ? ""
                                    : value.asText()
                    );
                }

                writer.write(
                        toCsvRow(values)
                );

                writer.newLine();
            }
        }
    }

    private void generateJsonReport(
            Path reportFile,
            JsonNode rowsNode
    ) throws IOException {

        try (BufferedWriter writer =
                     Files.newBufferedWriter(
                             reportFile,
                             StandardOpenOption.CREATE_NEW
                     )) {

            writer.write(
                    objectMapper.writeValueAsString(
                            rowsNode
                    )
            );

            writer.newLine();
        }
    }

    private String toCsvRow(
            List<String> values
    ) {

        return values.stream()
                .map(this::escapeCsvValue)
                .reduce(
                        (first, second) ->
                                first + "," + second
                )
                .orElse("");
    }

    private String escapeCsvValue(
            String value
    ) {

        if (value == null) {
            return "";
        }

        String escaped =
                value.replace(
                        "\"",
                        "\"\""
                );

        if (escaped.contains(",")
                || escaped.contains("\"")
                || escaped.contains("\n")
                || escaped.contains("\r")) {

            return "\"" + escaped + "\"";
        }

        return escaped;
    }
}