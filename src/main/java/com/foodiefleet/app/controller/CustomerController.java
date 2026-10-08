package com.foodiefleet.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    @GetMapping("/home")
    @ResponseBody
    public String customerHome() {
        return "<h1>Welcome to FoodieFleet!</h1><p>Customer Dashboard coming soon in Phase 3.</p>";
    }
}
