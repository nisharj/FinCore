package com.fincore.customer_service.dto;

public class InternalCustomerResponse {

    private Long id;
    private String email;

    public InternalCustomerResponse() {
    }

    public InternalCustomerResponse(Long id, String email) {
        this.id = id;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }
}