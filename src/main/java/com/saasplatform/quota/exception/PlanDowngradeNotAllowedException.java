package com.saasplatform.quota.exception;

import com.saasplatform.quota.entity.PlanType;
import org.springframework.http.HttpStatus;

public class PlanDowngradeNotAllowedException extends BusinessException {

    public PlanDowngradeNotAllowedException(PlanType from, PlanType to, long used, int targetLimit) {
        super(HttpStatus.CONFLICT, "PLAN_DOWNGRADE_NOT_ALLOWED",
                "Cannot downgrade " + from + " -> " + to + ": current month usage (" + used
                        + ") exceeds the target plan limit (" + targetLimit + ")");
    }
}
