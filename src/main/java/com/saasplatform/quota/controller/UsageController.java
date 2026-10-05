package com.saasplatform.quota.controller;

import com.saasplatform.quota.dto.DailyUsageResponse;
import com.saasplatform.quota.dto.ErrorResponse;
import com.saasplatform.quota.dto.PageResponse;
import com.saasplatform.quota.dto.UsageLogResponse;
import com.saasplatform.quota.dto.UsageRequest;
import com.saasplatform.quota.dto.UsageResponse;
import com.saasplatform.quota.service.UsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/users/{userId}/usage")
@Tag(name = "Usage", description = "Quota consumption and usage history")
public class UsageController {

    private final UsageService usageService;

    public UsageController(UsageService usageService) {
        this.usageService = usageService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Consume quota", description = "Logs one API/service usage event and deducts it from the monthly quota.")
    @ApiResponse(responseCode = "201", description = "Usage recorded")
    @ApiResponse(responseCode = "403", description = "SUBSCRIPTION_EXPIRED", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "429", description = "QUOTA_EXCEEDED", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public UsageResponse consume(@PathVariable Long userId, @Valid @RequestBody UsageRequest request) {
        return usageService.consume(userId, request);
    }

    @GetMapping("/logs")
    @Operation(summary = "Usage history", description = "Paged usage log, newest first.")
    public PageResponse<UsageLogResponse> logs(
            @PathVariable Long userId,
            @Parameter(description = "Zero based page index") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return usageService.history(userId, page, size);
    }

    @GetMapping("/daily")
    @Operation(summary = "Daily usage", description = "Quota units consumed per day in the current billing month (UTC). Days without usage are omitted.")
    public List<DailyUsageResponse> daily(@PathVariable Long userId) {
        return usageService.dailyUsage(userId);
    }
}
