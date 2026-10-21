package com.foodiefleet.app.repository;

import com.foodiefleet.app.model.Order;
import com.foodiefleet.app.model.OrderStatus;
import com.foodiefleet.app.model.Restaurant;
import com.foodiefleet.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerOrderByCreatedAtDesc(User customer);
    List<Order> findByRestaurantOrderByCreatedAtDesc(Restaurant restaurant);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByDriverOrderByCreatedAtDesc(User driver);

    // Ownership-scoped lookups: empty if the order doesn't belong to this owner's restaurant / this customer
    Optional<Order> findByIdAndRestaurant_Owner_Id(Long id, Long ownerId);
    Optional<Order> findByIdAndCustomer_Id(Long id, Long customerId);

    // Used to pre-fill the delivery address at checkout
    Optional<Order> findFirstByCustomerOrderByCreatedAtDesc(User customer);
}
