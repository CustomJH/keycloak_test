package com.example.usertest.domain.dto.auth;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Token response from Keycloak authentication")
public class TokenResponse {
    
    @JsonProperty("access_token")
    @Schema(description = "JWT Access token", example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;
    
    @JsonProperty("expires_in")
    @Schema(description = "Token expiration time in seconds", example = "3600")
    private Integer expiresIn;
    
    @JsonProperty("refresh_expires_in")
    @Schema(description = "Refresh token expiration time in seconds", example = "7200")
    private Integer refreshExpiresIn;
    
    @JsonProperty("refresh_token")
    @Schema(description = "Refresh token", example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;
    
    @JsonProperty("token_type")
    @Schema(description = "Token type", example = "Bearer")
    private String tokenType;
    
    @JsonProperty("not-before-policy")
    @Schema(description = "Not before policy", example = "0")
    private Integer notBeforePolicy;
    
    @JsonProperty("session_state")
    @Schema(description = "Session state", example = "12345678-1234-1234-1234-123456789012")
    private String sessionState;
    
    @Schema(description = "Token scope", example = "openid profile email")
    private String scope;
}