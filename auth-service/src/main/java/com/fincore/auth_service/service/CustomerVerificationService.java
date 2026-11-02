package com.fincore.auth_service.service;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.fincore.auth_service.dto.CustomerLookupResponse;
import com.fincore.auth_service.entity.CustomerVerification;
import com.fincore.auth_service.repository.CustomerVerificationRepo;

@Service
public class CustomerVerificationService {

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    private final CustomerVerificationRepo verificationRepo;
    private final CustomerLookupService customerLookupService;
    private final OtpUtil otpUtil;
    private final EmailService emailService;

    public CustomerVerificationService(
            CustomerVerificationRepo verificationRepo,
            CustomerLookupService customerLookupService,
            OtpUtil otpUtil,
            EmailService emailService) {

        this.verificationRepo = verificationRepo;
        this.customerLookupService = customerLookupService;
        this.otpUtil = otpUtil;
        this.emailService = emailService;
    }

    public void sendOtp(String username, String email) {

        CustomerLookupResponse customer =
                customerLookupService.findByEmail(email);

        if (customer == null || customer.getId() == null
                || customer.getEmail() == null) {
            throw new IllegalArgumentException(
                    "Customer could not be verified");
        }

        CustomerVerification existing =
                verificationRepo
                        .findTopByUsernameAndUsedFalseOrderByIdDesc(username)
                        .orElse(null);

        if (existing != null) {

            long secondsSinceLastSent =
                    Duration.between(
                            existing.getLastSentAt(),
                            LocalDateTime.now())
                            .getSeconds();

            if (secondsSinceLastSent < RESEND_COOLDOWN_SECONDS) {
                throw new IllegalStateException(
                        "Please wait before requesting another OTP");
            }
        }

        verificationRepo.deleteByUsername(username);

        String code = otpUtil.generateCode();

        CustomerVerification verification =
                new CustomerVerification();

        verification.setUsername(username);
        verification.setCustomerId(customer.getId());
        verification.setCodeHash(
                otpUtil.hash(username, code));
        verification.setExpiresAt(
                LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        verification.setLastSentAt(LocalDateTime.now());
        verification.setAttempts(0);
        verification.setUsed(false);

        verificationRepo.save(verification);

        emailService.sendVerificationCode(
                customer.getEmail(),
                code);
    }
}