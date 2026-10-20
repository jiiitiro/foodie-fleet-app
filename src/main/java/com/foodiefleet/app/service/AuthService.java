package com.foodiefleet.app.service;

import com.foodiefleet.app.dto.request.RegisterRequest;
import com.foodiefleet.app.model.Role;
import com.foodiefleet.app.model.User;
import com.foodiefleet.app.model.VerificationToken;
import com.foodiefleet.app.repository.UserRepository;
import com.foodiefleet.app.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // Admins are never self-registered; they are created by an existing admin or a startup seeder
    private static final Set<Role> SELF_REGISTRABLE_ROLES =
            EnumSet.of(Role.CUSTOMER, Role.RESTAURANT_OWNER, Role.DRIVER);

    @Transactional
    public User registerUser(RegisterRequest request) {
        if (!SELF_REGISTRABLE_ROLES.contains(request.getRole())) {
            throw new IllegalArgumentException("Selected role is not available for registration.");
        }

        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered!");
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .role(request.getRole())
                .enabled(false) // Account starts disabled until token is verified
                .build();

        User savedUser = userRepository.save(user);

        // Generate verification token
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = VerificationToken.builder()
                .token(token)
                .user(savedUser)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .build();

        tokenRepository.save(verificationToken);

        // Trigger email asynchronously (does not block registration HTTP response)
        emailService.sendVerificationEmail(savedUser.getEmail(), token);

        return savedUser;
    }

    @Transactional
    public boolean verifyToken(String token) {
        return tokenRepository.findByToken(token)
                .map(verToken -> {
                    if (verToken.getExpiryDate().isBefore(LocalDateTime.now())) {
                        return false;
                    }
                    User user = verToken.getUser();
                    user.setEnabled(true);
                    userRepository.save(user);
                    tokenRepository.delete(verToken);
                    return true;
                })
                .orElse(false);
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
