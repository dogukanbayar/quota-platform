package com.saasplatform.quota.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "usage_logs", indexes = @Index(name = "idx_usage_sub_period", columnList = "subscription_id, usage_period"))
public class UsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(nullable = false, length = 120)
    private String operation;

    @Column(name = "quota_used", nullable = false)
    private int quotaUsed;

    /** Billing month in yyyy-MM format, e.g. 2026-10. */
    @Column(name = "usage_period", nullable = false, length = 7)
    private String usagePeriod;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UsageLog() {
        // required by JPA
    }

    public UsageLog(Subscription subscription, String operation, int quotaUsed, String usagePeriod, Instant createdAt) {
        this.subscription = subscription;
        this.operation = operation;
        this.quotaUsed = quotaUsed;
        this.usagePeriod = usagePeriod;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public String getOperation() {
        return operation;
    }

    public int getQuotaUsed() {
        return quotaUsed;
    }

    public String getUsagePeriod() {
        return usagePeriod;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
