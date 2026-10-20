package com.foodiefleet.app.model;

import java.util.List;

public enum OrderStatus {
    PLACED,
    ACCEPTED,
    PREPARING,
    READY_FOR_PICKUP,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED;

    // Order lifecycle (docs/02-architecture.md#order-lifecycle): which statuses each role may move an order to
    public List<OrderStatus> nextStatusesFor(Role actor) {
        return switch (actor) {
            case RESTAURANT_OWNER -> switch (this) {
                case PLACED -> List.of(ACCEPTED, CANCELLED);
                case ACCEPTED -> List.of(PREPARING, CANCELLED);
                case PREPARING -> List.of(READY_FOR_PICKUP);
                default -> List.of();
            };
            case CUSTOMER -> this == PLACED ? List.of(CANCELLED) : List.of();
            case DRIVER -> switch (this) {
                case READY_FOR_PICKUP -> List.of(OUT_FOR_DELIVERY);
                case OUT_FOR_DELIVERY -> List.of(DELIVERED);
                default -> List.of();
            };
            case ADMIN -> List.of();
        };
    }

    public boolean canTransitionTo(OrderStatus next, Role actor) {
        return nextStatusesFor(actor).contains(next);
    }
}
