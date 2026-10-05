package com.saasplatform.quota.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "A single API/service usage event")
public record UsageRequest(
        @Schema(description = "Operation / endpoint that consumed quota", example = "/v1/summarize")
        @NotBlank @Size(max = 120) String operation,
        @Schema(description = "Quota units consumed by this call", example = "1", defaultValue = "1")
        @Min(1) @Max(100000) Integer units) {

    public int unitsOrDefault() {
        return units == null ? 1 : units;
    }
}
