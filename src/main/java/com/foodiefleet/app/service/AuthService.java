package com.foodiefleet.app.service;

import com.foodiefleet.app.dto.request.RegisterRequest;
import com.foodiefleet.app.model.User;
import com.foodiefleet.app.model.VerificationToken;
import com.foodiefleet.app.repository.UserRepository;
import com.foodiefleet.app.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered!");
        }

        User user = User.builder()
                .email(request.getEmail())
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
}
