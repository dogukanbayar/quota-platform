package com.saasplatform.quota.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Result of a successful quota consumption")
public record UsageResponse(
        @Schema(description = "Usage log id", example = "42") Long logId,
        @Schema(description = "Operation", example = "/v1/summarize") String operation,
        @Schema(description = "Units consumed by this call", example = "1") int unitsConsumed,
        @Schema(description = "Quota used in the current month", example = "43") long usedThisMonth,
        @Schema(description = "Quota remaining in the current month", example = "57") long remaining,
        @Schema(description = "Monthly limit", example = "100") int monthlyLimit,
        @Schema(description = "Time of the call") Instant createdAt) {
}
