package com.fincore.auth_service.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.fincore.auth_service.entity.User;
import com.fincore.auth_service.repository.UserRepo;

@Service
public class AuthService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepo userRepo,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public User register(User user) {

        user.setPassword(
            passwordEncoder.encode(user.getPassword())
        );

        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("USER");
        }

        return userRepo.save(user);
    }

    public String login(String username, String password){
        User user = userRepo.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        return jwtService.generateToken(user);
    }
}