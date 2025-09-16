package com.example.usertest.domain.dto.auth;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Internal DTO for Keycloak token request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeycloakTokenRequest {
    
    private String grantType;
    private String clientId;
    private String clientSecret;
    private String username;
    private String password;
    private String scope;
    
    public static KeycloakTokenRequest fromLogin(LoginRequest loginRequest, String clientId, String clientSecret) {
        return KeycloakTokenRequest.builder()
                .grantType("password")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .username(loginRequest.getUsername())
                .password(loginRequest.getPassword())
                .scope("openid profile email")
                .build();
    }
}