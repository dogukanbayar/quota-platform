package com.saasplatform.quota.dto;

import com.saasplatform.quota.entity.PlanType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload used to register a user together with the first subscription")
public record CreateUserRequest(
        @Schema(description = "Full name", example = "Ayse Yilmaz")
        @NotBlank @Size(max = 120) String fullName,
        @Schema(description = "Unique e-mail address", example = "ayse@example.com")
        @NotBlank @Email @Size(max = 180) String email,
        @Schema(description = "Initial plan, defaults to FREE when omitted", example = "FREE")
        PlanType planType) {
}
