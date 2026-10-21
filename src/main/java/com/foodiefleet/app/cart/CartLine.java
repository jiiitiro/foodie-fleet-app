package com.foodiefleet.app.cart;

import com.foodiefleet.app.model.MenuItem;

import java.math.BigDecimal;

public record CartLine(MenuItem item, int quantity, BigDecimal lineTotal) {

    public boolean available() {
        return item.isAvailable();
    }
}
