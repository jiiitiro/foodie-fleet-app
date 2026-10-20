package com.foodiefleet.app.controller;

import com.foodiefleet.app.dto.request.MenuItemRequest;
import com.foodiefleet.app.dto.request.RestaurantRequest;
import com.foodiefleet.app.model.OrderStatus;
import com.foodiefleet.app.model.Restaurant;
import com.foodiefleet.app.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/restaurant")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    // Helper method to resolve current logged in user ID from authentication principal
    private Long getCurrentUserId(UserDetails userDetails) {
        // Replace with your actual CustomUserDetails or User retrieval logic
        return 1L; // Placeholder: ensure you map userDetails.getUsername() to your User entity ID
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Long ownerId = getCurrentUserId(userDetails);
        Restaurant restaurant = restaurantService.getRestaurantByOwner(ownerId);

        if (restaurant == null) {
            model.addAttribute("restaurantRequest", new RestaurantRequest());
            return "restaurant/setup";
        }

        model.addAttribute("restaurant", restaurant);
        model.addAttribute("menuItems", restaurantService.getMenuItems(restaurant.getId()));
        model.addAttribute("orders", restaurantService.getIncomingOrders(restaurant.getId()));
        return "restaurant/dashboard";
    }

    @PostMapping("/profile")
    public String saveProfile(@AuthenticationPrincipal UserDetails userDetails,
                              @Valid @ModelAttribute RestaurantRequest request) {
        Long ownerId = getCurrentUserId(userDetails);
        restaurantService.saveOrUpdateProfile(ownerId, request);
        return "redirect:/restaurant/dashboard";
    }

    @PostMapping("/menu/add")
    public String addMenuItem(@RequestParam Long restaurantId,
                              @Valid @ModelAttribute MenuItemRequest request) {
        restaurantService.addMenuItem(restaurantId, request);
        return "redirect:/restaurant/dashboard";
    }

    @PostMapping("/menu/edit/{id}")
    public String editMenuItem(@PathVariable Long id,
                               @Valid @ModelAttribute MenuItemRequest request) {
        restaurantService.updateMenuItem(id, request);
        return "redirect:/restaurant/dashboard";
    }

    @PostMapping("/menu/toggle/{id}")
    public String toggleAvailability(@PathVariable Long id) {
        restaurantService.toggleItemAvailability(id);
        return "redirect:/restaurant/dashboard";
    }

    @PostMapping("/menu/delete/{id}")
    public String deleteMenuItem(@PathVariable Long id) {
        restaurantService.deleteMenuItem(id);
        return "redirect:/restaurant/dashboard";
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam OrderStatus status) {
        restaurantService.updateOrderStatus(id, status);
        return "redirect:/restaurant/dashboard";
    }
}
