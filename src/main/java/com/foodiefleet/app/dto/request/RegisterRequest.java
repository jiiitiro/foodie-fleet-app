package com.foodiefleet.app.dto.request;

import com.foodiefleet.app.model.Role;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    private String email;
    private String password;
    private String fullName;
    private String phoneNumber;
    private Role role;
}
