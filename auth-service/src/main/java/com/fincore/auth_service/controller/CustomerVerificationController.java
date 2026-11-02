package com.fincore.auth_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.fincore.auth_service.dto.CustomerVerificationRequest;
import com.fincore.auth_service.service.CustomerVerificationService;

@RestController
@RequestMapping("/auth/customer-verification")
public class CustomerVerificationController {

    private final CustomerVerificationService verificationService;

    public CustomerVerificationController(
            CustomerVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendOtp(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CustomerVerificationRequest request) {

        String username = jwt.getSubject();

        verificationService.sendOtp(
                username,
                request.getEmail());

        return ResponseEntity.ok(
                "Verification OTP sent successfully");
    }
}