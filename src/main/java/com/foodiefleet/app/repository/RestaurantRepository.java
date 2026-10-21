package com.foodiefleet.app.repository;

import com.foodiefleet.app.model.Restaurant;
import com.foodiefleet.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    Optional<Restaurant> findByOwner(User owner);

    // Customer browsing: only restaurants that are open
    List<Restaurant> findByActiveTrueOrderByNameAsc();

    List<Restaurant> findByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(String name);
}
