package com.saasplatform.quota.repository;

import com.saasplatform.quota.entity.UsageLog;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsageLogRepository extends JpaRepository<UsageLog, Long> {

    @Query("select coalesce(sum(u.quotaUsed), 0L) from UsageLog u "
            + "where u.subscription.id = :subscriptionId and u.usagePeriod = :period")
    long sumQuotaUsed(@Param("subscriptionId") Long subscriptionId, @Param("period") String period);

    Page<UsageLog> findBySubscriptionIdOrderByCreatedAtDescIdDesc(Long subscriptionId, Pageable pageable);

    List<UsageLog> findBySubscriptionIdAndUsagePeriod(Long subscriptionId, String usagePeriod);
}
