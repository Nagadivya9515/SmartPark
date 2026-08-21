package com.parking.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank @Size(min = 3, max = 50)
    private String username;

    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 8)
    private String password;
    
    public String getUsername()
    {
    	return username;
    }
    
    public String getEmail()
    {
    	return email;
    }
    
    public String getPassword()
    {
    	return password;
    }
}
