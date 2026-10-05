package com.saasplatform.quota.service;

import com.saasplatform.quota.dto.CreateUserRequest;
import com.saasplatform.quota.dto.UserResponse;
import com.saasplatform.quota.entity.AppUser;
import com.saasplatform.quota.entity.Plan;
import com.saasplatform.quota.entity.PlanType;
import com.saasplatform.quota.entity.Subscription;
import com.saasplatform.quota.exception.DuplicateResourceException;
import com.saasplatform.quota.exception.ResourceNotFoundException;
import com.saasplatform.quota.mapper.UserMapper;
import com.saasplatform.quota.repository.PlanRepository;
import com.saasplatform.quota.repository.UserRepository;
import com.saasplatform.quota.util.SubscriptionPeriods;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final UserMapper userMapper;
    private final Clock clock;

    public UserService(UserRepository userRepository, PlanRepository planRepository, UserMapper userMapper, Clock clock) {
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.userMapper = userMapper;
        this.clock = clock;
    }

    /** Registers a user and creates their single subscription in the same transaction. */
    @Transactional
    public UserResponse register(CreateUserRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("A user with e-mail " + email + " already exists");
        }
        PlanType planType = request.planType() == null ? PlanType.FREE : request.planType();
        Plan plan = planRepository.findByType(planType)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", planType));

        Instant now = clock.instant();
        AppUser user = new AppUser(request.fullName().trim(), email, now);
        user.setSubscription(new Subscription(user, plan, now, SubscriptionPeriods.endFrom(now)));
        return userMapper.toResponse(userRepository.save(user), now);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        AppUser user = userRepository.findByIdWithSubscription(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return userMapper.toResponse(user, clock.instant());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        Instant now = clock.instant();
        return userRepository.findAllWithSubscription().stream()
                .map(user -> userMapper.toResponse(user, now))
                .toList();
    }
}
