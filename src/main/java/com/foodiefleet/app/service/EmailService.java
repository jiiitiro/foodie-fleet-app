package com.foodiefleet.app.service;

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

    @Value("${APP_BASE_URL}")
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
}
