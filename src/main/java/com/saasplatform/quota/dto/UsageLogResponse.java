package com.saasplatform.quota.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "One usage log entry")
public record UsageLogResponse(
        @Schema(description = "Log id", example = "42") Long id,
        @Schema(description = "Operation", example = "/v1/summarize") String operation,
        @Schema(description = "Units consumed", example = "1") int quotaUsed,
        @Schema(description = "Billing period", example = "2026-10") String period,
        @Schema(description = "Time of the call") Instant createdAt) {
}
