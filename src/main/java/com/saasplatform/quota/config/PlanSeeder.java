package com.saasplatform.quota.config;

import com.saasplatform.quota.entity.Plan;
import com.saasplatform.quota.entity.PlanType;
import com.saasplatform.quota.repository.PlanRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Idempotently creates the three catalogue plans on start-up. */
@Component
@Order(1)
public class PlanSeeder implements ApplicationRunner {

    private final PlanRepository planRepository;

    public PlanSeeder(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Plan> catalogue = List.of(
                new Plan(PlanType.FREE, 100, BigDecimal.ZERO, "Get started: 100 API calls per month"),
                new Plan(PlanType.PRO, 10_000, new BigDecimal("29.90"), "Growing teams: 10,000 API calls per month"),
                new Plan(PlanType.ENTERPRISE, 1_000_000, new BigDecimal("299.00"), "Scale: 1,000,000 API calls per month"));
        catalogue.stream()
                .filter(plan -> !planRepository.existsByType(plan.getType()))
                .forEach(planRepository::save);
    }
}
