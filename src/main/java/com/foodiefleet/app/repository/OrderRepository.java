package com.foodiefleet.app.repository;

import com.foodiefleet.app.model.Order;
import com.foodiefleet.app.model.OrderStatus;
import com.foodiefleet.app.model.Restaurant;
import com.foodiefleet.app.model.User;
import java.util.List;

public interface OrderRepository {
    List<Order> findByCustomerOrderByCreatedAtDesc(User customer);
    List<Order> findByRestaurantOrderByCreatedAtDesc(Restaurant restaurant);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByDriverOrderByCreatedAtDesc(User driver);
}