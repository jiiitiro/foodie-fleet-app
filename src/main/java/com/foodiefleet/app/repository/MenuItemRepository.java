package com.foodiefleet.app.repository;

import com.foodiefleet.app.model.MenuItem;
import com.foodiefleet.app.model.Restaurant;
import java.util.List;

public interface MenuItemRepository {
    List<MenuItem> findByRestaurangAndAvailableTrue(Restaurant restaurant);
}