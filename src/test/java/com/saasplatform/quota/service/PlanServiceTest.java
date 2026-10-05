package com.saasplatform.quota.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.saasplatform.quota.TestData;
import com.saasplatform.quota.dto.PlanResponse;
import com.saasplatform.quota.entity.PlanType;
import com.saasplatform.quota.mapper.PlanMapper;
import com.saasplatform.quota.repository.PlanRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    private PlanRepository planRepository;

    @Test
    void findAllReturnsPlansOrderedByLimit() {
        when(planRepository.findAll()).thenReturn(List.of(TestData.pro(), TestData.free()));

        List<PlanResponse> plans = new PlanService(planRepository, new PlanMapper()).findAll();

        assertThat(plans).extracting(PlanResponse::type).containsExactly(PlanType.FREE, PlanType.PRO);
    }
}
