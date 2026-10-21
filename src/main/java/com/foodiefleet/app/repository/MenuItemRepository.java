package com.foodiefleet.app.repository;

import com.foodiefleet.app.model.MenuItem;
import com.foodiefleet.app.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByRestaurantAndAvailableTrueOrderByNameAsc(Restaurant restaurant);

    List<MenuItem> findByRestaurantOrderByNameAsc(Restaurant restaurant);

    // Ownership-scoped lookup: empty if the item doesn't belong to this owner's restaurant
    Optional<MenuItem> findByIdAndRestaurant_Owner_Id(Long id, Long ownerId);
}
