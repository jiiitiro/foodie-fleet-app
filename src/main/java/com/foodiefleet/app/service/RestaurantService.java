package com.foodiefleet.app.service;

import com.foodiefleet.app.dto.request.MenuItemRequest;
import com.foodiefleet.app.dto.request.RestaurantRequest;
import com.foodiefleet.app.exception.ConflictException;
import com.foodiefleet.app.exception.ResourceNotFoundException;
import com.foodiefleet.app.model.MenuItem;
import com.foodiefleet.app.model.Order;
import com.foodiefleet.app.model.OrderStatus;
import com.foodiefleet.app.model.Restaurant;
import com.foodiefleet.app.model.Role;
import com.foodiefleet.app.model.User;
import com.foodiefleet.app.repository.MenuItemRepository;
import com.foodiefleet.app.repository.OrderItemRepository;
import com.foodiefleet.app.repository.OrderRepository;
import com.foodiefleet.app.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

// Every operation is scoped to the given owner. Resources belonging to another owner are
// reported as "not found" (404) so their existence isn't revealed (SECURITY.md S2).
@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    // --- RESTAURANT PROFILE MANAGEMENT ---

    @Transactional(readOnly = true)
    public Optional<Restaurant> findRestaurant(User owner) {
        return restaurantRepository.findByOwner(owner);
    }

    @Transactional
    public Restaurant saveOrUpdateProfile(User owner, RestaurantRequest request) {
        Restaurant restaurant = restaurantRepository.findByOwner(owner)
                .orElseGet(() -> Restaurant.builder().owner(owner).build());

        restaurant.setName(request.getName());
        restaurant.setDescription(request.getDescription());
        restaurant.setAddress(request.getAddress());
        restaurant.setPhone(request.getPhone());
        restaurant.setActive(request.isActive());

        return restaurantRepository.save(restaurant);
    }

    @Transactional
    public Restaurant toggleOpen(User owner) {
        Restaurant restaurant = requireRestaurant(owner);
        restaurant.setActive(!Boolean.TRUE.equals(restaurant.getActive()));
        return restaurant;
    }

    // --- MENU ITEM CRUD ---

    @Transactional(readOnly = true)
    public List<MenuItem> getMenuItems(Restaurant restaurant) {
        return menuItemRepository.findByRestaurantOrderByNameAsc(restaurant);
    }

    @Transactional(readOnly = true)
    public MenuItem getMenuItem(User owner, Long itemId) {
        return requireOwnedMenuItem(owner, itemId);
    }

    @Transactional
    public MenuItem addMenuItem(User owner, MenuItemRequest request) {
        MenuItem item = MenuItem.builder()
                .restaurant(requireRestaurant(owner))
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .available(request.isAvailable())
                .build();

        return menuItemRepository.save(item);
    }

    @Transactional
    public MenuItem updateMenuItem(User owner, Long itemId, MenuItemRequest request) {
        MenuItem item = requireOwnedMenuItem(owner, itemId);

        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setImageUrl(request.getImageUrl());
        item.setAvailable(request.isAvailable());

        return item;
    }

    @Transactional
    public void toggleItemAvailability(User owner, Long itemId) {
        MenuItem item = requireOwnedMenuItem(owner, itemId);
        item.setAvailable(!item.isAvailable());
    }

    @Transactional
    public void deleteMenuItem(User owner, Long itemId) {
        MenuItem item = requireOwnedMenuItem(owner, itemId);
        // Past orders reference the item (and its price at purchase), so it can't be removed
        if (orderItemRepository.existsByMenuItem(item)) {
            throw new ConflictException("\"" + item.getName()
                    + "\" appears in past orders and can't be deleted. Mark it unavailable instead.");
        }
        item.getRestaurant().getMenuItems().remove(item);
        menuItemRepository.delete(item);
    }

    // --- RESTAURANT DASHBOARD ORDERS ---

    @Transactional(readOnly = true)
    public List<Order> getIncomingOrders(Restaurant restaurant) {
        return orderRepository.findByRestaurantOrderByCreatedAtDesc(restaurant);
    }

    @Transactional
    public Order updateOrderStatus(User owner, Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findByIdAndRestaurant_Owner_Id(orderId, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (!order.getStatus().canTransitionTo(newStatus, Role.RESTAURANT_OWNER)) {
            throw new ConflictException("Order #" + orderId + " can't move from "
                    + order.getStatus() + " to " + newStatus + ".");
        }
        order.setStatus(newStatus);
        return order;
    }

    // --- HELPERS ---

    private Restaurant requireRestaurant(User owner) {
        return restaurantRepository.findByOwner(owner)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant for owner", owner.getId()));
    }

    private MenuItem requireOwnedMenuItem(User owner, Long itemId) {
        return menuItemRepository.findByIdAndRestaurant_Owner_Id(itemId, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu item", itemId));
    }
}
