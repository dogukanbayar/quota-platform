package com.saasplatform.quota.mapper;

import com.saasplatform.quota.dto.PlanResponse;
import com.saasplatform.quota.entity.Plan;
import org.springframework.stereotype.Component;

@Component
public class PlanMapper {

    public PlanResponse toResponse(Plan plan) {
        return new PlanResponse(plan.getId(), plan.getType(), plan.getMonthlyLimit(),
                plan.getMonthlyPrice(), plan.getDescription());
    }
}
