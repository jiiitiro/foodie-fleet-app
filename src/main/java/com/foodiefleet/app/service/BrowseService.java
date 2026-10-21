package com.foodiefleet.app.service;

import com.foodiefleet.app.exception.ResourceNotFoundException;
import com.foodiefleet.app.model.MenuItem;
import com.foodiefleet.app.model.Restaurant;
import com.foodiefleet.app.repository.MenuItemRepository;
import com.foodiefleet.app.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Read-only catalogue for customers
@Service
@RequiredArgsConstructor
public class BrowseService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;

    @Transactional(readOnly = true)
    public List<Restaurant> findOpenRestaurants(String query) {
        if (query == null || query.isBlank()) {
            return restaurantRepository.findByActiveTrueOrderByNameAsc();
        }
        return restaurantRepository.findByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(query.trim());
    }

    @Transactional(readOnly = true)
    public Restaurant getRestaurant(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", id));
    }

    @Transactional(readOnly = true)
    public List<MenuItem> getAvailableMenu(Restaurant restaurant) {
        return menuItemRepository.findByRestaurantAndAvailableTrueOrderByNameAsc(restaurant);
    }
}
