package com.saasplatform.quota.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

@Schema(description = "Standard error payload returned by every failing endpoint")
public record ErrorResponse(
        @Schema(description = "Time the error occurred (UTC)") Instant timestamp,
        @Schema(description = "HTTP status code", example = "429") int status,
        @Schema(description = "HTTP reason phrase", example = "Too Many Requests") String error,
        @Schema(description = "Stable machine-readable error code", example = "QUOTA_EXCEEDED") String code,
        @Schema(description = "Human readable message") String message,
        @Schema(description = "Request path", example = "/api/v1/users/1/usage") String path,
        @Schema(description = "Field level validation errors (only for 400 validation failures)") Map<String, String> validationErrors) {
}
