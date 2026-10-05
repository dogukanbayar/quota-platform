package com.saasplatform.quota.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saasplatform.quota.TestData;
import com.saasplatform.quota.dto.CreateUserRequest;
import com.saasplatform.quota.dto.UserResponse;
import com.saasplatform.quota.entity.AppUser;
import com.saasplatform.quota.entity.PlanType;
import com.saasplatform.quota.entity.SubscriptionStatus;
import com.saasplatform.quota.exception.DuplicateResourceException;
import com.saasplatform.quota.exception.ResourceNotFoundException;
import com.saasplatform.quota.mapper.UserMapper;
import com.saasplatform.quota.repository.PlanRepository;
import com.saasplatform.quota.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PlanRepository planRepository;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, planRepository, new UserMapper(), TestData.clock());
    }

    @Test
    void registerCreatesFreeSubscriptionByDefaultAndNormalisesEmail() {
        when(userRepository.existsByEmail("ayse@example.com")).thenReturn(false);
        when(planRepository.findByType(PlanType.FREE)).thenReturn(Optional.of(TestData.free()));
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = service.register(new CreateUserRequest("  Ayse  ", " Ayse@Example.com ", null));

        assertThat(response.email()).isEqualTo("ayse@example.com");
        assertThat(response.fullName()).isEqualTo("Ayse");
        assertThat(response.planType()).isEqualTo(PlanType.FREE);
        assertThat(response.subscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void registerHonoursRequestedPlan() {
        when(userRepository.existsByEmail("pro@example.com")).thenReturn(false);
        when(planRepository.findByType(PlanType.PRO)).thenReturn(Optional.of(TestData.pro()));
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = service.register(new CreateUserRequest("Pro User", "pro@example.com", PlanType.PRO));

        assertThat(response.planType()).isEqualTo(PlanType.PRO);
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);
        CreateUserRequest request = new CreateUserRequest("Dup", "dup@example.com", null);

        assertThatThrownBy(() -> service.register(request)).isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    void registerFailsWhenPlanIsNotSeeded() {
        when(userRepository.existsByEmail("x@example.com")).thenReturn(false);
        when(planRepository.findByType(PlanType.FREE)).thenReturn(Optional.empty());
        CreateUserRequest request = new CreateUserRequest("X", "x@example.com", null);

        assertThatThrownBy(() -> service.register(request)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByIdReturnsUser() {
        AppUser user = TestData.subscription(TestData.free()).getUser();
        when(userRepository.findByIdWithSubscription(1L)).thenReturn(Optional.of(user));

        assertThat(service.getById(1L).email()).isEqualTo("test@example.com");
    }

    @Test
    void getByIdFailsWhenMissing() {
        when(userRepository.findByIdWithSubscription(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(7L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findAllMapsEveryUser() {
        AppUser user = TestData.subscription(TestData.pro()).getUser();
        when(userRepository.findAllWithSubscription()).thenReturn(List.of(user));

        assertThat(service.findAll()).hasSize(1);
        assertThat(service.findAll().get(0).planType()).isEqualTo(PlanType.PRO);
    }
}
