package com.saasplatform.quota.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Quota units consumed on one day (UTC)")
public record DailyUsageResponse(
        @Schema(description = "Day (UTC)", example = "2026-10-04") LocalDate date,
        @Schema(description = "Units consumed that day", example = "37") long units) {
}
