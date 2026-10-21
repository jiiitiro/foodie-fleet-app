package com.foodiefleet.app.controller;

import com.foodiefleet.app.cart.Cart;
import com.foodiefleet.app.cart.CartService;
import com.foodiefleet.app.dto.request.CreateOrderRequest;
import com.foodiefleet.app.exception.ConflictException;
import com.foodiefleet.app.model.Order;
import com.foodiefleet.app.model.User;
import com.foodiefleet.app.security.CurrentUserService;
import com.foodiefleet.app.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CartController {

    private static final String REDIRECT_CART = "redirect:/customer/cart";

    private final Cart cart;
    private final CartService cartService;
    private final OrderService orderService;
    private final CurrentUserService currentUserService;

    @GetMapping("/cart")
    public String cart(Model model) {
        if (!model.containsAttribute("createOrderRequest")) {
            String lastAddress = orderService.lastDeliveryAddress(currentUserService.get()).orElse("");
            model.addAttribute("createOrderRequest", new CreateOrderRequest(lastAddress));
        }
        model.addAttribute("cart", cartService.view());
        model.addAttribute("maxQuantity", CartService.MAX_QUANTITY_PER_ITEM);
        return "customer/cart";
    }

    @PostMapping("/cart/update")
    public String update(@RequestParam Long menuItemId, @RequestParam int quantity, RedirectAttributes redirect) {
        try {
            cartService.update(menuItemId, quantity);
        } catch (ConflictException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return REDIRECT_CART;
    }

    @PostMapping("/cart/remove")
    public String remove(@RequestParam Long menuItemId) {
        cartService.remove(menuItemId);
        return REDIRECT_CART;
    }

    @PostMapping("/cart/clear")
    public String clear(RedirectAttributes redirect) {
        cartService.clear();
        redirect.addFlashAttribute("success", "Your cart is empty.");
        return REDIRECT_CART;
    }

    @PostMapping("/checkout")
    public String checkout(@Valid @ModelAttribute("createOrderRequest") CreateOrderRequest request,
                           BindingResult bindingResult, Model model, RedirectAttributes redirect) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("cart", cartService.view());
            model.addAttribute("maxQuantity", CartService.MAX_QUANTITY_PER_ITEM);
            return "customer/cart";
        }

        User customer = currentUserService.get();
        try {
            Order order = orderService.placeOrder(customer, cart.getRestaurantId(), cart.getQuantities(),
                    request.getDeliveryAddress());
            cart.clear();
            redirect.addFlashAttribute("success", "Order placed! We've sent a confirmation to " + customer.getEmail() + ".");
            return "redirect:/customer/orders/" + order.getId();
        } catch (ConflictException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return REDIRECT_CART;
        }
    }
}
