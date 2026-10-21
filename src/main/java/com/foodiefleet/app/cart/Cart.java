package com.foodiefleet.app.cart;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

// The customer's cart, one per HTTP session. Holds only item ids and quantities: names and
// prices are always read from the database, so the client can never influence what is charged.
@Component
@SessionScope
public class Cart implements Serializable {

    private Long restaurantId;
    private final Map<Long, Integer> quantities = new LinkedHashMap<>();

    public Long getRestaurantId() {
        return restaurantId;
    }

    public Map<Long, Integer> getQuantities() {
        return Collections.unmodifiableMap(quantities);
    }

    public boolean isEmpty() {
        return quantities.isEmpty();
    }

    public int getItemCount() {
        return quantities.values().stream().mapToInt(Integer::intValue).sum();
    }

    void add(Long restaurantId, Long menuItemId, int quantity) {
        this.restaurantId = restaurantId;
        quantities.merge(menuItemId, quantity, Integer::sum);
    }

    void set(Long menuItemId, int quantity) {
        if (quantity <= 0) {
            remove(menuItemId);
        } else if (quantities.containsKey(menuItemId)) {
            quantities.put(menuItemId, quantity);
        }
    }

    void remove(Long menuItemId) {
        quantities.remove(menuItemId);
        if (quantities.isEmpty()) {
            restaurantId = null;
        }
    }

    public void clear() {
        quantities.clear();
        restaurantId = null;
    }
}
