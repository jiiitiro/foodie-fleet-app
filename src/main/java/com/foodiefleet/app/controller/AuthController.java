package com.foodiefleet.app.controller;

import com.foodiefleet.app.dto.request.RegisterRequest;
import com.foodiefleet.app.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("registerRequest") RegisterRequest request, Model model) {
        try {
            authService.registerUser(request);
            return "redirect:/auth/login?registered=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";
        }
    }

    // Redirects user to their role-specific landing page after login
    @GetMapping("/default-redirect")
    public String defaultAfterLogin(Authentication authentication) {
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_CUSTOMER"))) {
            return "redirect:/customer/home";
        } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_RESTAURANT_OWNER"))) {
            return "redirect:/restaurant/dashboard";
        } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_DRIVER"))) {
            return "redirect:/driver/deliveries";
        } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return "redirect:/admin/dashboard";
        }
        return "redirect:/";
    }
}
