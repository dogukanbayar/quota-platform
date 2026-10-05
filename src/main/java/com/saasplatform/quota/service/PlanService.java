package com.saasplatform.quota.service;

import com.saasplatform.quota.dto.PlanResponse;
import com.saasplatform.quota.mapper.PlanMapper;
import com.saasplatform.quota.repository.PlanRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanService {

    private final PlanRepository planRepository;
    private final PlanMapper planMapper;

    public PlanService(PlanRepository planRepository, PlanMapper planMapper) {
        this.planRepository = planRepository;
        this.planMapper = planMapper;
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> findAll() {
        return planRepository.findAll().stream()
                .sorted((a, b) -> Integer.compare(a.getMonthlyLimit(), b.getMonthlyLimit()))
                .map(planMapper::toResponse)
                .toList();
    }
}
