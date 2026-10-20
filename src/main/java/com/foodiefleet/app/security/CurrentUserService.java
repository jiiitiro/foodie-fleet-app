package com.foodiefleet.app.security;

import com.foodiefleet.app.model.User;
import com.foodiefleet.app.repository.UserRepository;
import com.foodiefleet.app.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Resolves the logged-in user's entity; the principal's username is the user's email
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user");
        }
        return userRepository.findByEmail(AuthService.normalizeEmail(authentication.getName()))
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }
}
