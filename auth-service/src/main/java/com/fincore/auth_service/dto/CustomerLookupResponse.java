
package com.fincore.auth_service.dto;

public class CustomerLookupResponse {

    private Long id;
    private String email;

    public CustomerLookupResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
