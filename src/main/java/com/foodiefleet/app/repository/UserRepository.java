package com.foodiefleet.app.repository;

import com.foodiefleet.app.model.Role;
import com.foodiefleet.app.model.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<User> fingByEmail(String email);
    Boolean existsByEmail(String email);
    List<User> fingByRole(Role role);
}