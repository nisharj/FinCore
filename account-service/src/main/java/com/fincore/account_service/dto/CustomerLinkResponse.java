package com.fincore.account_service.dto;

public class CustomerLinkResponse {

    private Long customerId;
    private boolean verified;

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified;}
}