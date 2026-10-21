package com.foodiefleet.app.cart;

import com.foodiefleet.app.model.Restaurant;

import java.math.BigDecimal;
import java.util.List;

// Cart contents resolved against current database prices and availability
public record CartView(Restaurant restaurant, List<CartLine> lines, BigDecimal total) {

    public static CartView empty() {
        return new CartView(null, List.of(), BigDecimal.ZERO);
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public int itemCount() {
        return lines.stream().mapToInt(CartLine::quantity).sum();
    }

    public boolean hasUnavailableItems() {
        return lines.stream().anyMatch(line -> !line.available());
    }

    public boolean restaurantOpen() {
        return restaurant != null && Boolean.TRUE.equals(restaurant.getActive());
    }

    public boolean canCheckout() {
        return !isEmpty() && restaurantOpen() && !hasUnavailableItems();
    }
}
