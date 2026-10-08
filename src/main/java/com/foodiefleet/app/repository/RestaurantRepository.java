package com.foodiefleet.app.repository;

import com.foodiefleet.app.model.Restaurant;
import com.foodiefleet.app.model.User;
import java.util.Optional;

public interface RestaurantRepository {
    Optional<Restaurant> findByOwner(User owner);

}