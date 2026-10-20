package com.foodiefleet.app.repository;

import com.foodiefleet.app.model.MenuItem;
import com.foodiefleet.app.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByRestaurantAndAvailableTrue(Restaurant restaurant);

    List<MenuItem> findByRestaurantId(Long restaurantId);
}
