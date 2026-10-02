package com.parking.auth.dto;

public class AuthResponse {
    
    private String message;
    private String username;
    private String role;

    // No-Args Constructor
    public AuthResponse() {}

    // All-Args Constructor
    public AuthResponse(String message, String username, String role) {
        this.message = message;
        this.username = username;
        this.role = role;
    }

    // Getters and Setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}