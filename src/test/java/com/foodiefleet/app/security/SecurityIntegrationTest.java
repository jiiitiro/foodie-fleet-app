package com.foodiefleet.app.security;

import com.foodiefleet.app.TestcontainersConfiguration;
import com.foodiefleet.app.model.Role;
import com.foodiefleet.app.model.User;
import com.foodiefleet.app.repository.UserRepository;
import com.foodiefleet.app.repository.VerificationTokenRepository;
import com.foodiefleet.app.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private VerificationTokenRepository tokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EmailService emailService;

    @BeforeEach
    void cleanDatabase() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    // --- Registration -------------------------------------------------------

    @Test
    void register_withAdminRole_isRejected() throws Exception {
        mockMvc.perform(post("/auth/register").with(csrf())
                        .param("fullName", "Mallory")
                        .param("email", "mallory@example.com")
                        .param("password", "password123")
                        .param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("errorMessage"));

        assertThat(userRepository.findByEmail("mallory@example.com")).isEmpty();
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void register_withValidData_createsDisabledUserAndSendsEmail() throws Exception {
        mockMvc.perform(post("/auth/register").with(csrf())
                        .param("fullName", "Alice")
                        .param("email", "Alice@Example.com")
                        .param("password", "password123")
                        .param("role", "CUSTOMER"))
                .andExpect(redirectedUrl("/auth/login?registered=true"));

        User user = userRepository.findByEmail("alice@example.com").orElseThrow();
        assertThat(user.getEnabled()).isFalse();
        assertThat(user.getRole()).isEqualTo(Role.CUSTOMER);
        verify(emailService).sendVerificationEmail(eq("alice@example.com"), anyString());
    }

    @Test
    void register_withInvalidData_showsFieldErrors() throws Exception {
        mockMvc.perform(post("/auth/register").with(csrf())
                        .param("fullName", "")
                        .param("email", "not-an-email")
                        .param("password", "short")
                        .param("role", "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "fullName", "email", "password"));

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void register_withoutCsrfToken_isForbidden() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .param("fullName", "Alice")
                        .param("email", "alice@example.com")
                        .param("password", "password123")
                        .param("role", "CUSTOMER"))
                .andExpect(status().isForbidden());

        assertThat(userRepository.count()).isZero();
    }

    // --- Login ---------------------------------------------------------------

    @Test
    void login_withUnverifiedAccount_redirectsWithDisabledError() throws Exception {
        saveUser("pending@example.com", Role.CUSTOMER, false);

        mockMvc.perform(formLogin("/auth/login").user("pending@example.com").password("password123"))
                .andExpect(redirectedUrl("/auth/login?error=disabled"));
    }

    @Test
    void login_withWrongPassword_redirectsWithGenericError() throws Exception {
        saveUser("bob@example.com", Role.CUSTOMER, true);

        mockMvc.perform(formLogin("/auth/login").user("bob@example.com").password("wrong-password"))
                .andExpect(redirectedUrl("/auth/login?error=true"));
    }

    @Test
    void login_withVerifiedAccount_redirectsToRoleLanding() throws Exception {
        saveUser("bob@example.com", Role.CUSTOMER, true);

        mockMvc.perform(formLogin("/auth/login").user("Bob@Example.com").password("password123"))
                .andExpect(redirectedUrl("/auth/default-redirect"));
    }

    @Test
    void loginPage_explainsUnverifiedAccount() throws Exception {
        mockMvc.perform(get("/auth/login").param("error", "disabled"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("not verified yet")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Invalid email or password"))));
    }

    // --- Route authorization -------------------------------------------------

    @Test
    void anonymousUser_isRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/customer/home"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customer_cannotAccessRestaurantArea() throws Exception {
        mockMvc.perform(get("/restaurant/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customer_cannotAccessAdminArea() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void driver_canAccessDriverArea() throws Exception {
        mockMvc.perform(get("/driver/deliveries"))
                .andExpect(status().isOk());
    }

    // --- Error handling ------------------------------------------------------

    @Test
    @WithMockUser(roles = "RESTAURANT_OWNER")
    void missingResource_rendersNotFoundPage() throws Exception {
        mockMvc.perform(post("/restaurant/menu/toggle/999999").with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(content().string(containsString("Menu item not found with id 999999")));
    }

    private void saveUser(String email, Role role, boolean enabled) {
        userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode("password123"))
                .fullName("Test User")
                .role(role)
                .enabled(enabled)
                .build());
    }
}
