package com.foodiefleet.app.cart;

import com.foodiefleet.app.exception.ConflictException;
import com.foodiefleet.app.exception.ResourceNotFoundException;
import com.foodiefleet.app.model.MenuItem;
import com.foodiefleet.app.model.Restaurant;
import com.foodiefleet.app.repository.MenuItemRepository;
import com.foodiefleet.app.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    public static final int MAX_QUANTITY_PER_ITEM = 20;

    private final Cart cart;
    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;

    @Transactional(readOnly = true)
    public MenuItem add(Long menuItemId, int quantity) {
        MenuItem item = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item", menuItemId));
        Restaurant restaurant = item.getRestaurant();

        if (!Boolean.TRUE.equals(restaurant.getActive())) {
            throw new ConflictException(restaurant.getName() + " is closed right now.");
        }
        if (!item.isAvailable()) {
            throw new ConflictException("\"" + item.getName() + "\" is not available right now.");
        }
        // One restaurant per cart: orders go to a single kitchen and a single driver
        if (!cart.isEmpty() && !restaurant.getId().equals(cart.getRestaurantId())) {
            String current = restaurantRepository.findById(cart.getRestaurantId())
                    .map(Restaurant::getName).orElse("another restaurant");
            throw new ConflictException("Your cart has items from " + current
                    + ". Check out or clear your cart before ordering from " + restaurant.getName() + ".");
        }
        int newQuantity = cart.getQuantities().getOrDefault(menuItemId, 0) + quantity;
        if (quantity < 1 || newQuantity > MAX_QUANTITY_PER_ITEM) {
            throw new ConflictException("You can order between 1 and " + MAX_QUANTITY_PER_ITEM + " of each item.");
        }

        cart.add(restaurant.getId(), menuItemId, quantity);
        return item;
    }

    public void update(Long menuItemId, int quantity) {
        if (quantity > MAX_QUANTITY_PER_ITEM) {
            throw new ConflictException("You can order up to " + MAX_QUANTITY_PER_ITEM + " of each item.");
        }
        cart.set(menuItemId, quantity);
    }

    public void remove(Long menuItemId) {
        cart.remove(menuItemId);
    }

    public void clear() {
        cart.clear();
    }

    public int itemCount() {
        return cart.getItemCount();
    }

    @Transactional(readOnly = true)
    public CartView view() {
        if (cart.isEmpty()) {
            return CartView.empty();
        }
        Restaurant restaurant = restaurantRepository.findById(cart.getRestaurantId()).orElse(null);
        if (restaurant == null) {
            cart.clear();
            return CartView.empty();
        }

        Map<Long, MenuItem> items = menuItemRepository.findAllById(cart.getQuantities().keySet()).stream()
                .collect(Collectors.toMap(MenuItem::getId, Function.identity()));

        List<CartLine> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : cart.getQuantities().entrySet()) {
            MenuItem item = items.get(entry.getKey());
            if (item == null) {
                continue; // deleted from the menu since it was added
            }
            BigDecimal lineTotal = item.getPrice().multiply(BigDecimal.valueOf(entry.getValue()));
            lines.add(new CartLine(item, entry.getValue(), lineTotal));
            if (item.isAvailable()) {
                total = total.add(lineTotal);
            }
        }
        if (lines.isEmpty()) {
            cart.clear();
            return CartView.empty();
        }
        return new CartView(restaurant, lines, total);
    }
}
