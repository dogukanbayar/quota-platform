package com.saasplatform.quota.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "plans")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private PlanType type;

    @Column(name = "monthly_limit", nullable = false)
    private int monthlyLimit;

    @Column(name = "monthly_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(length = 255)
    private String description;

    /** Plan 1 - N Subscription. No cascade: removing a plan must never remove subscriptions. */
    @OneToMany(mappedBy = "plan", fetch = FetchType.LAZY)
    private List<Subscription> subscriptions = new ArrayList<>();

    protected Plan() {
        // required by JPA
    }

    public Plan(PlanType type, int monthlyLimit, BigDecimal monthlyPrice, String description) {
        this.type = type;
        this.monthlyLimit = monthlyLimit;
        this.monthlyPrice = monthlyPrice;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public PlanType getType() {
        return type;
    }

    public int getMonthlyLimit() {
        return monthlyLimit;
    }

    public BigDecimal getMonthlyPrice() {
        return monthlyPrice;
    }

    public String getDescription() {
        return description;
    }

    public List<Subscription> getSubscriptions() {
        return subscriptions;
    }
}
