package com.saasplatform.quota.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saasplatform.quota.config.DemoDataSeeder;
import com.saasplatform.quota.dto.CreateUserRequest;
import com.saasplatform.quota.dto.UsageRequest;
import com.saasplatform.quota.dto.UserResponse;
import com.saasplatform.quota.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DemoDataSeederTest {

    @Mock
    private UserService userService;
    @Mock
    private UsageService usageService;
    @Mock
    private UserRepository userRepository;

    @Test
    void seedsThreeUsersWhenDatabaseIsEmpty() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userService.register(any(CreateUserRequest.class)))
                .thenReturn(new UserResponse(1L, "n", "e", null, null, null));

        new DemoDataSeeder(userService, usageService, userRepository).run(null);

        verify(userService, times(3)).register(any(CreateUserRequest.class));
        verify(usageService, times(3)).consume(anyLong(), any(UsageRequest.class));
    }

    @Test
    void skipsUsersThatAlreadyExist() {
        when(userRepository.existsByEmail(any())).thenReturn(true);

        new DemoDataSeeder(userService, usageService, userRepository).run(null);

        verify(userService, never()).register(any(CreateUserRequest.class));
    }
}
