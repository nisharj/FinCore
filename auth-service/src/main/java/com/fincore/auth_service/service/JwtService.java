package com.fincore.auth_service.service;


import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.fincore.auth_service.entity.User;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
    private SecretKey secretKey = Keys.hmacShaKeyFor(
        "fincore-super-secret-key-which-is-long-enough-123456".getBytes()
    );

    public String generateToken(User user){
        return Jwts.builder()
            .subject(user.getUsername())
            .claim("role", user.getRole())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 1 hour expiration
            .signWith(secretKey)
            .compact();
        
    }
}
