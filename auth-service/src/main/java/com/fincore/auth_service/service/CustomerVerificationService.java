package com.fincore.auth_service.service;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import com.fincore.auth_service.dto.CustomerLookupResponse;
import com.fincore.auth_service.entity.CustomerVerification;
import com.fincore.auth_service.repository.CustomerVerificationRepo;
import com.fincore.auth_service.entity.User;
import com.fincore.auth_service.repository.UserRepo;

import java.security.MessageDigest;

@Service
public class CustomerVerificationService {

        private static final int OTP_EXPIRY_MINUTES = 10;
        private static final int RESEND_COOLDOWN_SECONDS = 60;

        private final CustomerVerificationRepo verificationRepo;
        private final CustomerLookupService customerLookupService;
        private final OtpUtil otpUtil;
        private final EmailService emailService;

        private final UserRepo userRepo;

        public CustomerVerificationService(
                        CustomerVerificationRepo verificationRepo,
                        CustomerLookupService customerLookupService,
                        OtpUtil otpUtil,
                        EmailService emailService, UserRepo userRepo) {

                this.verificationRepo = verificationRepo;
                this.customerLookupService = customerLookupService;
                this.otpUtil = otpUtil;
                this.emailService = emailService;
                this.userRepo = userRepo;
        }

        @Transactional
        public void sendOtp(String username, String email) {

                CustomerLookupResponse customer = customerLookupService.findByEmail(email);

                if (customer == null || customer.getId() == null
                                || customer.getEmail() == null) {
                        throw new IllegalArgumentException(
                                        "Customer could not be verified");
                }

                CustomerVerification existing = verificationRepo
                                .findTopByUsernameAndUsedFalseOrderByIdDesc(username)
                                .orElse(null);

                if (existing != null) {

                        long secondsSinceLastSent = Duration.between(
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

                CustomerVerification verification = new CustomerVerification();

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

        @Transactional
        public void verifyOtp(String username, String code) {

                CustomerVerification verification = verificationRepo
                                .findTopByUsernameAndUsedFalseOrderByIdDesc(username)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "No active OTP found. Please request a new OTP."));

                // Check expiry
                if (LocalDateTime.now().isAfter(verification.getExpiresAt())) {
                        verification.setUsed(true);
                        verificationRepo.save(verification);

                        throw new ResponseStatusException(
                                        HttpStatus.GONE,
                                        "OTP has expired. Please request a new OTP.");
                }

                // Maximum 5 attempts
                if (verification.getAttempts() >= 5) {
                        verification.setUsed(true);
                        verificationRepo.save(verification);

                        throw new ResponseStatusException(
                                        HttpStatus.TOO_MANY_REQUESTS,
                                        "Maximum OTP attempts exceeded. Request a new OTP.");
                }

                String submittedHash = otpUtil.hash(username, code);

                boolean matches = MessageDigest.isEqual(
                                verification.getCodeHash().getBytes(
                                                java.nio.charset.StandardCharsets.UTF_8),
                                submittedHash.getBytes(
                                                java.nio.charset.StandardCharsets.UTF_8));

                if (!matches) {

                        int attempts = verification.getAttempts() + 1;
                        verification.setAttempts(attempts);

                        if (attempts >= 5) {
                                verification.setUsed(true);
                        }

                        verificationRepo.save(verification);

                        throw new ResponseStatusException(
                                        attempts >= 5
                                                        ? HttpStatus.TOO_MANY_REQUESTS
                                                        : HttpStatus.BAD_REQUEST,
                                        attempts >= 5
                                                        ? "Maximum OTP attempts exceeded. Request a new OTP."
                                                        : "Invalid OTP. Please try again.");
                }

                // Find authenticated user
                User user = userRepo.findByUsername(username)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "User not found."));

                // Link the verified customer
                user.setCustomerId(verification.getCustomerId());
                user.setCustomerVerified(true);

                userRepo.save(user);

                // OTP can never be reused
                verification.setUsed(true);
                verificationRepo.save(verification);
        }
}