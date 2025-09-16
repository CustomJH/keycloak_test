package com.example.usertest.domain.dto.auth;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Login request with username and password")
public class LoginRequest {
    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Schema(description = "Username for authentication", example = "testuser")
    private String username;
    
    @NotBlank(message = "Password is required")
    @Size(min = 4, max = 100, message = "Password must be between 4 and 100 characters")
    @Schema(description = "Password for authentication", example = "password123")
    private String password;
}