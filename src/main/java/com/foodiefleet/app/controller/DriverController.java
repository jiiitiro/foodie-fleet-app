package com.foodiefleet.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/driver")
public class DriverController {

    @GetMapping("/deliveries")
    @ResponseBody
    public String driverDeliveries() {
        return "<h1>Driver Delivery Portal</h1><p>Active delivery jobs coming soon in Phase 3.</p>";
    }
}
