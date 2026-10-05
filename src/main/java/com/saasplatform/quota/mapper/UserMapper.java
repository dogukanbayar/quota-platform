package com.saasplatform.quota.mapper;

import com.saasplatform.quota.dto.UserResponse;
import com.saasplatform.quota.entity.AppUser;
import com.saasplatform.quota.entity.Subscription;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(AppUser user, Instant now) {
        Subscription sub = user.getSubscription();
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getCreatedAt(),
                sub == null ? null : sub.getPlan().getType(),
                sub == null ? null : sub.effectiveStatus(now));
    }
}
