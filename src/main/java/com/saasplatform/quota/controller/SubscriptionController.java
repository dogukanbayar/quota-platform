package com.saasplatform.quota.controller;

import com.saasplatform.quota.dto.ChangePlanRequest;
import com.saasplatform.quota.dto.ErrorResponse;
import com.saasplatform.quota.dto.SubscriptionResponse;
import com.saasplatform.quota.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/{userId}/subscription")
@Tag(name = "Subscriptions", description = "Subscription lifecycle: plan changes, renewal and expiry")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    @Operation(summary = "Get subscription", description = "Returns the subscription with this month's usage and remaining quota.")
    @ApiResponse(responseCode = "404", description = "Subscription not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public SubscriptionResponse get(@PathVariable Long userId) {
        return subscriptionService.getByUserId(userId);
    }

    @PutMapping("/plan")
    @Operation(summary = "Change plan (upgrade / downgrade)",
            description = "A downgrade is blocked with PLAN_DOWNGRADE_NOT_ALLOWED when this month's usage exceeds the target plan limit.")
    @ApiResponse(responseCode = "403", description = "Subscription expired", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Downgrade not allowed", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public SubscriptionResponse changePlan(@PathVariable Long userId, @Valid @RequestBody ChangePlanRequest request) {
        return subscriptionService.changePlan(userId, request.planType());
    }

    @PostMapping("/renew")
    @Operation(summary = "Renew an expired subscription", description = "Starts a new one-month term. Rejected while the subscription is still active.")
    public SubscriptionResponse renew(@PathVariable Long userId) {
        return subscriptionService.renew(userId);
    }

    @PostMapping("/expire")
    @Operation(summary = "Expire the subscription (demo / back-office)", description = "Ends the current term immediately so the expiry flow can be exercised.")
    public SubscriptionResponse expire(@PathVariable Long userId) {
        return subscriptionService.expire(userId);
    }
}
