package com.foodiefleet.app.service;

import com.foodiefleet.app.dto.request.RegisterRequest;
import com.foodiefleet.app.model.Role;
import com.foodiefleet.app.model.User;
import com.foodiefleet.app.repository.UserRepository;
import com.foodiefleet.app.repository.VerificationTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private VerificationTokenRepository tokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerUser_rejectsAdminRole() {
        RegisterRequest request = request("evil@example.com", Role.ADMIN);

        assertThatThrownBy(() -> authService.registerUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not available");

        verifyNoInteractions(userRepository, tokenRepository, emailService);
    }

    @Test
    void registerUser_rejectsMissingRole() {
        RegisterRequest request = request("someone@example.com", null);

        assertThatThrownBy(() -> authService.registerUser(request))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(userRepository);
    }

    @Test
    void registerUser_rejectsDuplicateEmail() {
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerUser(request("Taken@Example.com", Role.CUSTOMER)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_createsDisabledUserWithNormalizedEmailAndSendsVerification() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = authService.registerUser(request("  New@Example.COM ", Role.RESTAURANT_OWNER));

        assertThat(saved.getEmail()).isEqualTo("new@example.com");
        assertThat(saved.getPassword()).isEqualTo("hashed");
        assertThat(saved.getRole()).isEqualTo(Role.RESTAURANT_OWNER);
        assertThat(saved.getEnabled()).isFalse();
        verify(tokenRepository).save(any());
        verify(emailService).sendVerificationEmail(eq("new@example.com"), anyString());
    }

    private RegisterRequest request(String email, Role role) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword("password123");
        request.setFullName("Test User");
        request.setRole(role);
        return request;
    }
}
