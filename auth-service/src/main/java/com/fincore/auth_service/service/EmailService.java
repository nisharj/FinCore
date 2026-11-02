package com.fincore.auth_service.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationCode(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        message.setSubject("FinCore Customer Verification");
        message.setText(
            "Your FinCore verification code is: " + code
            + "\n\nThis code expires in 10 minutes."
            + "\nIf you did not request this code, ignore this email."
        );

        mailSender.send(message);
    }
}