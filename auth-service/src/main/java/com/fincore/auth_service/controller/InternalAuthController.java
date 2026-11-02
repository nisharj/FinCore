package com.fincore.auth_service.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fincore.auth_service.dto.CustomerLinkResponse;
import com.fincore.auth_service.entity.User;
import com.fincore.auth_service.repository.UserRepo;

@RestController
@RequestMapping("/auth/internal")
public class InternalAuthController {

    private final UserRepo userRepo;

    @Value("${fincore.internal.secret}")
    private String internalSecret;

    public InternalAuthController(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @GetMapping("/customer-link")
    public ResponseEntity<CustomerLinkResponse> getCustomerLink(
            @RequestParam String username, @RequestHeader("X-Internal-Secret") String suppliedSecret) {

        boolean validSecret = MessageDigest.isEqual(
                internalSecret.getBytes(StandardCharsets.UTF_8),
                suppliedSecret.getBytes(StandardCharsets.UTF_8)
        );

        if (!validSecret) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal secret");
        }

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return ResponseEntity.ok(new CustomerLinkResponse(user.getCustomerId(), user.isCustomerVerified()));
    }
}