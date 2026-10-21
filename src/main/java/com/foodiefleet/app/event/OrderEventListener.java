package com.foodiefleet.app.event;

import com.foodiefleet.app.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final EmailService emailService;

    // Only after commit: no confirmation email for an order that was rolled back
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(OrderPlacedEvent event) {
        emailService.sendOrderConfirmation(event);
    }
}
