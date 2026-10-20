package com.foodiefleet.app.service;

import com.foodiefleet.app.dto.request.MenuItemRequest;
import com.foodiefleet.app.dto.request.RestaurantRequest;
import com.foodiefleet.app.exception.ResourceNotFoundException;
import com.foodiefleet.app.model.MenuItem;
import com.foodiefleet.app.model.Order;
import com.foodiefleet.app.model.OrderStatus;
import com.foodiefleet.app.model.Restaurant;
import com.foodiefleet.app.model.User;
import com.foodiefleet.app.repository.MenuItemRepository;
import com.foodiefleet.app.repository.OrderRepository;
import com.foodiefleet.app.repository.RestaurantRepository;
import com.foodiefleet.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    // --- RESTAURANT PROFILE MANAGEMENT ---

    @Transactional(readOnly = true)
    public Restaurant getRestaurantByOwner(Long ownerId) {
        return restaurantRepository.findByOwnerId(ownerId).orElse(null);
    }

    @Transactional
    public Restaurant saveOrUpdateProfile(Long ownerId, RestaurantRequest request) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", ownerId));

        Restaurant restaurant = restaurantRepository.findByOwnerId(ownerId)
                .orElse(Restaurant.builder().owner(owner).build());

        restaurant.setName(request.getName());
        restaurant.setDescription(request.getDescription());
        restaurant.setAddress(request.getAddress());
        restaurant.setPhone(request.getPhone());
        restaurant.setActive(request.isActive());

        return restaurantRepository.save(restaurant);
    }

    // --- MENU ITEM CRUD ---

    @Transactional(readOnly = true)
    public List<MenuItem> getMenuItems(Long restaurantId) {
        return menuItemRepository.findByRestaurantId(restaurantId);
    }

    @Transactional
    public MenuItem addMenuItem(Long restaurantId, MenuItemRequest request) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));

        MenuItem item = MenuItem.builder()
                .restaurant(restaurant)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .available(request.isAvailable())
                .build();

        return menuItemRepository.save(item);
    }

    @Transactional
    public MenuItem updateMenuItem(Long itemId, MenuItemRequest request) {
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item", itemId));

        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setImageUrl(request.getImageUrl());
        item.setAvailable(request.isAvailable());

        return menuItemRepository.save(item);
    }

    @Transactional
    public void toggleItemAvailability(Long itemId) {
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item", itemId));
        item.setAvailable(!item.isAvailable());
        menuItemRepository.save(item);
    }

    @Transactional
    public void deleteMenuItem(Long itemId) {
        menuItemRepository.deleteById(itemId);
    }

    // --- RESTAURANT DASHBOARD ORDERS ---

    @Transactional(readOnly = true)
    public List<Order> getIncomingOrders(Long restaurantId) {
        return orderRepository.findByRestaurantId(restaurantId);
    }

    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        order.setStatus(newStatus);
        orderRepository.save(order);
    }
}
