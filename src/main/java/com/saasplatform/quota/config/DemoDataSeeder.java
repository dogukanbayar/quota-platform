package com.saasplatform.quota.config;

import com.saasplatform.quota.dto.CreateUserRequest;
import com.saasplatform.quota.dto.UsageRequest;
import com.saasplatform.quota.dto.UserResponse;
import com.saasplatform.quota.entity.PlanType;
import com.saasplatform.quota.repository.UserRepository;
import com.saasplatform.quota.service.UsageService;
import com.saasplatform.quota.service.UserService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Optional demo users so the dashboard is not empty on first launch (app.seed-demo=true). */
@Component
@Order(2)
@ConditionalOnProperty(name = "app.seed-demo", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private final UserService userService;
    private final UsageService usageService;
    private final UserRepository userRepository;

    public DemoDataSeeder(UserService userService, UsageService usageService, UserRepository userRepository) {
        this.userService = userService;
        this.usageService = usageService;
        this.userRepository = userRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed("Ayse Yilmaz", "ayse@example.com", PlanType.FREE, 37);
        seed("Mehmet Demir", "mehmet@example.com", PlanType.PRO, 2450);
        seed("Acme Corp", "platform@acme.io", PlanType.ENTERPRISE, 120_000);
    }

    private void seed(String name, String email, PlanType plan, int units) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        UserResponse user = userService.register(new CreateUserRequest(name, email, plan));
        usageService.consume(user.id(), new UsageRequest("/v1/bootstrap", units));
    }
}
