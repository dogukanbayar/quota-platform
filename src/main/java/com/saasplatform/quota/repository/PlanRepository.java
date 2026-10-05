package com.saasplatform.quota.repository;

import com.saasplatform.quota.entity.Plan;
import com.saasplatform.quota.entity.PlanType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    Optional<Plan> findByType(PlanType type);

    boolean existsByType(PlanType type);
}
