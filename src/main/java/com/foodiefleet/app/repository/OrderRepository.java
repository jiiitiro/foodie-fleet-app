package com.foodiefleet.app.repository;

import com.foodiefleet.app.model.Order;
import com.foodiefleet.app.model.OrderStatus;
import com.foodiefleet.app.model.Restaurant;
import com.foodiefleet.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerOrderByCreatedAtDesc(User customer);
    List<Order> findByRestaurantOrderByCreatedAtDesc(Restaurant restaurant);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByDriverOrderByCreatedAtDesc(User driver);

    List<Order> findByRestaurantId(Long restaurantId);
}