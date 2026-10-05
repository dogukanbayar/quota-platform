package com.saasplatform.quota.controller;

import com.saasplatform.quota.dto.PlanResponse;
import com.saasplatform.quota.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plans")
@Tag(name = "Plans", description = "Plan catalogue")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    @Operation(summary = "List plans", description = "Returns FREE, PRO and ENTERPRISE with their monthly API call limits, ordered by limit.")
    public List<PlanResponse> list() {
        return planService.findAll();
    }
}
