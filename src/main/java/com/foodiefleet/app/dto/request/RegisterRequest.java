package com.foodiefleet.app.dto.request;

import com.foodiefleet.app.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    // BCrypt only uses the first 72 bytes of a password
    @NotBlank
    @Size(min = 8, max = 72, message = "must be between 8 and 72 characters")
    private String password;

    @NotBlank
    @Size(max = 255)
    private String fullName;

    @Size(max = 30)
    private String phoneNumber;

    @NotNull
    private Role role;
}
