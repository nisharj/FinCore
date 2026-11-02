package com.fincore.auth_service.dto;

public class CustomerVerificationRequest {

    private String email;

    public CustomerVerificationRequest() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}