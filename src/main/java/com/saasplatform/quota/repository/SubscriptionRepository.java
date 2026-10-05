package com.saasplatform.quota.repository;

import com.saasplatform.quota.entity.Subscription;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    @EntityGraph(attributePaths = {"plan"})
    Optional<Subscription> findByUserId(Long userId);

    /** Row-level write lock: serialises concurrent quota consumption for the same subscription. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Subscription> findWithLockByUserId(Long userId);
}
