package com.foodiefleet.app.service;

import com.foodiefleet.app.event.OrderPlacedEvent;
import com.foodiefleet.app.exception.ConflictException;
import com.foodiefleet.app.exception.ResourceNotFoundException;
import com.foodiefleet.app.cart.CartService;
import com.foodiefleet.app.model.MenuItem;
import com.foodiefleet.app.model.Order;
import com.foodiefleet.app.model.OrderItem;
import com.foodiefleet.app.model.OrderStatus;
import com.foodiefleet.app.model.Restaurant;
import com.foodiefleet.app.model.Role;
import com.foodiefleet.app.model.User;
import com.foodiefleet.app.repository.MenuItemRepository;
import com.foodiefleet.app.repository.OrderRepository;
import com.foodiefleet.app.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final ApplicationEventPublisher eventPublisher;

    // Re-validates everything at checkout time and prices each line from the database
    @Transactional
    public Order placeOrder(User customer, Long restaurantId, Map<Long, Integer> quantities, String deliveryAddress) {
        if (restaurantId == null || quantities.isEmpty()) {
            throw new ConflictException("Your cart is empty.");
        }
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ConflictException("This restaurant is no longer on FoodieFleet."));
        if (!Boolean.TRUE.equals(restaurant.getActive())) {
            throw new ConflictException(restaurant.getName() + " is closed right now, so the order can't be placed.");
        }

        Map<Long, MenuItem> items = menuItemRepository.findAllById(quantities.keySet()).stream()
                .collect(Collectors.toMap(MenuItem::getId, Function.identity()));

        Order order = Order.builder()
                .customer(customer)
                .restaurant(restaurant)
                .deliveryAddress(deliveryAddress.trim())
                .totalPrice(BigDecimal.ZERO)
                .build();
        BigDecimal total = BigDecimal.ZERO;
        List<String> summary = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            MenuItem item = items.get(entry.getKey());
            int quantity = entry.getValue();
            if (item == null || !item.getRestaurant().getId().equals(restaurantId)) {
                throw new ConflictException("An item in your cart is no longer on the menu. Please review your cart.");
            }
            if (!item.isAvailable()) {
                throw new ConflictException("\"" + item.getName() + "\" is no longer available. Remove it from your cart to continue.");
            }
            if (quantity < 1 || quantity > CartService.MAX_QUANTITY_PER_ITEM) {
                throw new ConflictException("Invalid quantity for \"" + item.getName() + "\".");
            }

            order.getItems().add(OrderItem.builder()
                    .order(order)
                    .menuItem(item)
                    .quantity(quantity)
                    .priceAtPurchase(item.getPrice())
                    .build());
            BigDecimal lineTotal = item.getPrice().multiply(BigDecimal.valueOf(quantity));
            total = total.add(lineTotal);
            summary.add(quantity + " x " + item.getName() + "  " + lineTotal.setScale(2).toPlainString());
        }
        order.setTotalPrice(total);
        Order saved = orderRepository.save(order);

        eventPublisher.publishEvent(new OrderPlacedEvent(customer.getEmail(), customer.getFullName(), saved.getId(),
                restaurant.getName(), summary, total, saved.getDeliveryAddress()));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Order> getCustomerOrders(User customer) {
        return orderRepository.findByCustomerOrderByCreatedAtDesc(customer);
    }

    // Another customer's order is reported as not found (404)
    @Transactional(readOnly = true)
    public Order getCustomerOrder(User customer, Long orderId) {
        return orderRepository.findByIdAndCustomer_Id(orderId, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }

    @Transactional
    public Order cancelByCustomer(User customer, Long orderId) {
        Order order = getCustomerOrder(customer, orderId);
        if (!order.getStatus().isCancellableBy(Role.CUSTOMER)) {
            throw new ConflictException("Order #" + orderId + " can no longer be cancelled: the restaurant has already accepted it.");
        }
        order.setStatus(OrderStatus.CANCELLED);
        return order;
    }

    @Transactional(readOnly = true)
    public Optional<String> lastDeliveryAddress(User customer) {
        return orderRepository.findFirstByCustomerOrderByCreatedAtDesc(customer).map(Order::getDeliveryAddress);
    }
}
