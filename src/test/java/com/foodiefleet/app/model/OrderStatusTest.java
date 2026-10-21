package com.foodiefleet.app.model;

import org.junit.jupiter.api.Test;

import static com.foodiefleet.app.model.OrderStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @Test
    void owner_canMoveOrderThroughKitchenStages() {
        assertThat(PLACED.nextStatusesFor(Role.RESTAURANT_OWNER)).containsExactly(ACCEPTED, CANCELLED);
        assertThat(ACCEPTED.nextStatusesFor(Role.RESTAURANT_OWNER)).containsExactly(PREPARING, CANCELLED);
        assertThat(PREPARING.nextStatusesFor(Role.RESTAURANT_OWNER)).containsExactly(READY_FOR_PICKUP);
    }

    @Test
    void owner_cannotSkipStagesOrTakeOverDelivery() {
        assertThat(PLACED.canTransitionTo(DELIVERED, Role.RESTAURANT_OWNER)).isFalse();
        assertThat(PLACED.canTransitionTo(PREPARING, Role.RESTAURANT_OWNER)).isFalse();
        assertThat(PREPARING.canTransitionTo(CANCELLED, Role.RESTAURANT_OWNER)).isFalse();
        assertThat(READY_FOR_PICKUP.nextStatusesFor(Role.RESTAURANT_OWNER)).isEmpty();
        assertThat(OUT_FOR_DELIVERY.nextStatusesFor(Role.RESTAURANT_OWNER)).isEmpty();
    }

    @Test
    void driver_handlesPickupAndDelivery() {
        assertThat(READY_FOR_PICKUP.canTransitionTo(OUT_FOR_DELIVERY, Role.DRIVER)).isTrue();
        assertThat(OUT_FOR_DELIVERY.canTransitionTo(DELIVERED, Role.DRIVER)).isTrue();
        assertThat(PLACED.nextStatusesFor(Role.DRIVER)).isEmpty();
    }

    @Test
    void customer_canOnlyCancelBeforeAcceptance() {
        assertThat(PLACED.canTransitionTo(CANCELLED, Role.CUSTOMER)).isTrue();
        assertThat(ACCEPTED.canTransitionTo(CANCELLED, Role.CUSTOMER)).isFalse();
    }

    @Test
    void isCancellableBy_matchesTransitionRules() {
        assertThat(PLACED.isCancellableBy(Role.CUSTOMER)).isTrue();
        assertThat(ACCEPTED.isCancellableBy(Role.CUSTOMER)).isFalse();
        assertThat(ACCEPTED.isCancellableBy(Role.RESTAURANT_OWNER)).isTrue();
        assertThat(OUT_FOR_DELIVERY.isCancellableBy(Role.DRIVER)).isFalse();
    }

    @Test
    void terminalStatuses_haveNoNextStatus() {
        for (Role role : Role.values()) {
            assertThat(DELIVERED.nextStatusesFor(role)).isEmpty();
            assertThat(CANCELLED.nextStatusesFor(role)).isEmpty();
        }
    }
}
