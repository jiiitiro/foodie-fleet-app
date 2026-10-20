package com.foodiefleet.app.repository;

import com.foodiefleet.app.model.MenuItem;
import com.foodiefleet.app.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    boolean existsByMenuItem(MenuItem menuItem);
}
