package com.foodiefleet.app.controller;

import com.foodiefleet.app.cart.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// Cart badge in the navbar on customer pages
@ControllerAdvice(assignableTypes = {CustomerController.class, CartController.class})
@RequiredArgsConstructor
public class CustomerModelAdvice {

    private final CartService cartService;

    @ModelAttribute("cartCount")
    public int cartCount() {
        return cartService.itemCount();
    }
}
