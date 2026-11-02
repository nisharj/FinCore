package com.fincore.auth_service.dto;

public class CustomerLinkResponse {

    private Long customerId;
    private boolean verified;

    public CustomerLinkResponse(Long customerId, boolean verified) {
        this.customerId = customerId;
        this.verified = verified;
    }

    public Long getCustomerId() {return customerId; }

    public boolean isVerified() { return verified; }
}