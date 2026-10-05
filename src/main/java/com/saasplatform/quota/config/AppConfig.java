package com.saasplatform.quota.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    /** Injectable clock so that time based rules (expiry, billing month) are testable. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public OpenAPI quotaPlatformOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Quota Platform API")
                .version("1.0.0")
                .description("SaaS subscription management and monthly quota tracking. "
                        + "Plans: FREE, PRO, ENTERPRISE. Every usage call is logged and deducted from the monthly quota.")
                .contact(new Contact().name("Quota Platform Team")));
    }
}
