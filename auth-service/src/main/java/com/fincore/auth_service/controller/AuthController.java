package com.fincore.auth_service.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fincore.auth_service.dto.LoginRequest;
import com.fincore.auth_service.dto.LoginResponse;
import com.fincore.auth_service.dto.RegisterResponse;
import com.fincore.auth_service.entity.User;
import com.fincore.auth_service.service.AuthService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody User user) {
        User savedUser = authService.register(user);

        RegisterResponse response = new RegisterResponse(
            savedUser.getId(),
            savedUser.getUsername(),
            savedUser.getRole()
        );
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {

        String token = authService.login(
                request.getUsername(),
                request.getPassword()
        );

        return ResponseEntity.ok(new LoginResponse(token));
    }

}
    