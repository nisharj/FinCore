
package com.fincore.auth_service.service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OtpUtil {

    private final SecureRandom random = new SecureRandom();
    private final byte[] hashKey;

    public OtpUtil(@Value("${otp.hash-secret}") String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException(
                    "otp.hash-secret must be at least 32 characters");
        }
        this.hashKey = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String generateCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    public String hash(String username, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hashKey, "HmacSHA256"));
            byte[] input = (username + ":" + code)
                    .getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(mac.doFinal(input));
        } catch (Exception e) {
            throw new IllegalStateException("Could not hash OTP", e);
        }
    }
}
