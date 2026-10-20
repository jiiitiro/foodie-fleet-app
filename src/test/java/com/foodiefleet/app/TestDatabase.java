package com.foodiefleet.app;

import org.springframework.jdbc.core.JdbcTemplate;

// Integration test classes share one Spring context (and Postgres container), so each test starts from empty tables
public final class TestDatabase {

    private TestDatabase() {
    }

    public static void truncateAll(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("TRUNCATE order_items, orders, menu_items, restaurants, verification_tokens, users "
                + "RESTART IDENTITY CASCADE");
    }
}
