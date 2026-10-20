package com.foodiefleet.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    // Placeholder until M3 (customer ordering)
    @GetMapping("/home")
    public String customerHome() {
        return "customer/home";
    }
}
