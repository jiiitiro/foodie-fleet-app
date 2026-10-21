package com.foodiefleet.app.event;

import java.math.BigDecimal;
import java.util.List;

// Published inside the checkout transaction; carries plain values so listeners never touch lazy entities
public record OrderPlacedEvent(String customerEmail, String customerName, Long orderId,
                               String restaurantName, List<String> lines, BigDecimal total,
                               String deliveryAddress) {
}
