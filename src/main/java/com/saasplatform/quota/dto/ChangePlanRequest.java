package com.saasplatform.quota.dto;

import com.saasplatform.quota.entity.PlanType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Plan change request (upgrade or downgrade)")
public record ChangePlanRequest(
        @Schema(description = "Target plan", example = "PRO") @NotNull PlanType planType) {
}
