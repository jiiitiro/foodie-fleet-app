package com.foodiefleet.app.service;

import com.foodiefleet.app.event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async // Runs on a background thread pool automatically
    public void sendVerificationEmail(String recipientEmail, String token) {
        String confirmationUrl = baseUrl + "/auth/verify?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(recipientEmail);
        message.setSubject("FoodieFleet - Verify Your Account");
        message.setText("Welcome to FoodieFleet!\n\nPlease click the link below to verify your email address:\n" + confirmationUrl);

        mailSender.send(message);
    }

    @Async
    public void sendOrderConfirmation(OrderPlacedEvent order) {
        String orderUrl = baseUrl + "/customer/orders/" + order.orderId();
        String text = "Hi " + order.customerName() + ",\n\n"
                + "Thanks for your order! " + order.restaurantName() + " has received it.\n\n"
                + String.join("\n", order.lines()) + "\n"
                + "Total: " + order.total().setScale(2).toPlainString() + "\n\n"
                + "Delivering to: " + order.deliveryAddress() + "\n\n"
                + "Track your order: " + orderUrl + "\n\n"
                + "FoodieFleet";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(order.customerEmail());
        message.setSubject("FoodieFleet - Order #" + order.orderId() + " confirmed");
        message.setText(text);

        mailSender.send(message);
    }
}
