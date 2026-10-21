package com.foodiefleet.app.controller;

import com.foodiefleet.app.cart.CartService;
import com.foodiefleet.app.exception.ConflictException;
import com.foodiefleet.app.model.Order;
import com.foodiefleet.app.model.OrderStatus;
import com.foodiefleet.app.model.Restaurant;
import com.foodiefleet.app.model.Role;
import com.foodiefleet.app.security.CurrentUserService;
import com.foodiefleet.app.service.BrowseService;
import com.foodiefleet.app.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

    // Progress tracker steps shown on the order page (CANCELLED is shown separately)
    private static final List<OrderStatus> TRACKER_STEPS = List.of(OrderStatus.PLACED, OrderStatus.ACCEPTED,
            OrderStatus.PREPARING, OrderStatus.READY_FOR_PICKUP, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED);

    private final BrowseService browseService;
    private final CartService cartService;
    private final OrderService orderService;
    private final CurrentUserService currentUserService;

    // --- BROWSE ---

    @GetMapping("/home")
    public String customerHome(@RequestParam(name = "q", required = false) String query, Model model) {
        model.addAttribute("restaurants", browseService.findOpenRestaurants(query));
        model.addAttribute("query", query);
        return "customer/home";
    }

    @GetMapping("/restaurants/{id}")
    public String restaurant(@PathVariable Long id, Model model) {
        Restaurant restaurant = browseService.getRestaurant(id);
        model.addAttribute("restaurant", restaurant);
        model.addAttribute("menuItems", browseService.getAvailableMenu(restaurant));
        model.addAttribute("cart", cartService.view());
        model.addAttribute("maxQuantity", CartService.MAX_QUANTITY_PER_ITEM);
        return "customer/restaurant";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Long menuItemId,
                            @RequestParam(defaultValue = "1") int quantity,
                            @RequestParam Long restaurantId, RedirectAttributes redirect) {
        try {
            var item = cartService.add(menuItemId, quantity);
            redirect.addFlashAttribute("success", "Added " + quantity + " × " + item.getName() + " to your cart.");
        } catch (ConflictException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customer/restaurants/" + restaurantId;
    }

    // --- ORDERS ---

    @GetMapping("/orders")
    public String orders(Model model) {
        model.addAttribute("orders", orderService.getCustomerOrders(currentUserService.get()));
        model.addAttribute("customerRole", Role.CUSTOMER);
        return "customer/orders";
    }

    @GetMapping("/orders/{id}")
    public String order(@PathVariable Long id, Model model) {
        Order order = orderService.getCustomerOrder(currentUserService.get(), id);
        model.addAttribute("order", order);
        model.addAttribute("steps", TRACKER_STEPS);
        model.addAttribute("customerRole", Role.CUSTOMER);
        return "customer/order";
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            orderService.cancelByCustomer(currentUserService.get(), id);
            redirect.addFlashAttribute("success", "Order #" + id + " has been cancelled.");
        } catch (ConflictException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customer/orders/" + id;
    }
}
