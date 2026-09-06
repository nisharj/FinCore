package com.fincore.auth_service.dto;

public class RegisterResponse {
    private long id;
    private String username;
    private String role;

    public RegisterResponse(long id, String username, String role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    public long getId() { return id; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public void setId(long id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setRole(String role) { this.role = role; }

}
