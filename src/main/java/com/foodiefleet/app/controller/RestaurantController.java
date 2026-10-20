package com.foodiefleet.app.controller;

import com.foodiefleet.app.dto.request.MenuItemRequest;
import com.foodiefleet.app.dto.request.RestaurantRequest;
import com.foodiefleet.app.exception.ConflictException;
import com.foodiefleet.app.model.MenuItem;
import com.foodiefleet.app.model.Order;
import com.foodiefleet.app.model.OrderStatus;
import com.foodiefleet.app.model.Restaurant;
import com.foodiefleet.app.model.Role;
import com.foodiefleet.app.model.User;
import com.foodiefleet.app.security.CurrentUserService;
import com.foodiefleet.app.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Controller
@RequestMapping("/restaurant")
@RequiredArgsConstructor
public class RestaurantController {

    private static final String REDIRECT_DASHBOARD = "redirect:/restaurant/dashboard";
    private static final Set<OrderStatus> FINISHED_STATUSES = EnumSet.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED);

    private final RestaurantService restaurantService;
    private final CurrentUserService currentUserService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Optional<Restaurant> restaurant = restaurantService.findRestaurant(currentUserService.get());

        // First login: the owner has to create the restaurant profile before anything else
        if (restaurant.isEmpty()) {
            model.addAttribute("restaurantRequest", new RestaurantRequest());
            return "restaurant/setup";
        }

        populateDashboard(model, restaurant.get());
        return "restaurant/dashboard";
    }

    // --- PROFILE ---

    @PostMapping("/profile")
    public String saveProfile(@Valid @ModelAttribute("restaurantRequest") RestaurantRequest request,
                              BindingResult bindingResult, Model model, RedirectAttributes redirect) {
        User owner = currentUserService.get();
        if (bindingResult.hasErrors()) {
            Optional<Restaurant> restaurant = restaurantService.findRestaurant(owner);
            if (restaurant.isEmpty()) {
                return "restaurant/setup";
            }
            populateDashboard(model, restaurant.get());
            return "restaurant/dashboard";
        }

        restaurantService.saveOrUpdateProfile(owner, request);
        redirect.addFlashAttribute("success", "Restaurant profile saved.");
        return REDIRECT_DASHBOARD;
    }

    @PostMapping("/toggle-open")
    public String toggleOpen(RedirectAttributes redirect) {
        Restaurant restaurant = restaurantService.toggleOpen(currentUserService.get());
        redirect.addFlashAttribute("success",
                restaurant.getActive() ? "Your restaurant is now open." : "Your restaurant is now closed.");
        return REDIRECT_DASHBOARD;
    }

    // --- MENU ---

    @PostMapping("/menu/add")
    public String addMenuItem(@Valid @ModelAttribute("menuItemRequest") MenuItemRequest request,
                              BindingResult bindingResult, Model model, RedirectAttributes redirect) {
        User owner = currentUserService.get();
        if (bindingResult.hasErrors()) {
            Optional<Restaurant> restaurant = restaurantService.findRestaurant(owner);
            if (restaurant.isEmpty()) {
                return REDIRECT_DASHBOARD;
            }
            populateDashboard(model, restaurant.get());
            return "restaurant/dashboard";
        }

        MenuItem item = restaurantService.addMenuItem(owner, request);
        redirect.addFlashAttribute("success", "Added \"" + item.getName() + "\" to the menu.");
        return REDIRECT_DASHBOARD;
    }

    @GetMapping("/menu/edit/{id}")
    public String editMenuItemForm(@PathVariable Long id, Model model) {
        MenuItem item = restaurantService.getMenuItem(currentUserService.get(), id);
        model.addAttribute("itemId", id);
        model.addAttribute("menuItemRequest", MenuItemRequest.from(item));
        return "restaurant/menu-edit";
    }

    @PostMapping("/menu/edit/{id}")
    public String editMenuItem(@PathVariable Long id,
                               @Valid @ModelAttribute("menuItemRequest") MenuItemRequest request,
                               BindingResult bindingResult, Model model, RedirectAttributes redirect) {
        User owner = currentUserService.get();
        if (bindingResult.hasErrors()) {
            restaurantService.getMenuItem(owner, id); // 404 if the item isn't this owner's
            model.addAttribute("itemId", id);
            return "restaurant/menu-edit";
        }

        MenuItem item = restaurantService.updateMenuItem(owner, id, request);
        redirect.addFlashAttribute("success", "Updated \"" + item.getName() + "\".");
        return REDIRECT_DASHBOARD;
    }

    @PostMapping("/menu/toggle/{id}")
    public String toggleAvailability(@PathVariable Long id) {
        restaurantService.toggleItemAvailability(currentUserService.get(), id);
        return REDIRECT_DASHBOARD;
    }

    @PostMapping("/menu/delete/{id}")
    public String deleteMenuItem(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            restaurantService.deleteMenuItem(currentUserService.get(), id);
            redirect.addFlashAttribute("success", "Menu item deleted.");
        } catch (ConflictException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return REDIRECT_DASHBOARD;
    }

    // --- ORDERS ---

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam OrderStatus status, RedirectAttributes redirect) {
        try {
            restaurantService.updateOrderStatus(currentUserService.get(), id, status);
            redirect.addFlashAttribute("success", "Order #" + id + " is now " + status + ".");
        } catch (ConflictException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return REDIRECT_DASHBOARD;
    }

    // Keeps form objects already in the model (e.g. ones with validation errors)
    private void populateDashboard(Model model, Restaurant restaurant) {
        List<MenuItem> menuItems = restaurantService.getMenuItems(restaurant);
        List<Order> orders = restaurantService.getIncomingOrders(restaurant);
        model.addAttribute("restaurant", restaurant);
        model.addAttribute("menuItems", menuItems);
        model.addAttribute("orders", orders);
        model.addAttribute("activeOrderCount",
                orders.stream().filter(order -> !FINISHED_STATUSES.contains(order.getStatus())).count());
        model.addAttribute("availableItemCount", menuItems.stream().filter(MenuItem::isAvailable).count());
        model.addAttribute("ownerRole", Role.RESTAURANT_OWNER);
        if (!model.containsAttribute("restaurantRequest")) {
            model.addAttribute("restaurantRequest", RestaurantRequest.from(restaurant));
        }
        if (!model.containsAttribute("menuItemRequest")) {
            model.addAttribute("menuItemRequest", new MenuItemRequest());
        }
    }
}
