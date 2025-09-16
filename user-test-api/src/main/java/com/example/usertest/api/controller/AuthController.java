package com.example.usertest.api.controller;

import com.example.usertest.api.config.properties.KeycloakProperties;
import com.example.usertest.api.service.auth.KeycloakTokenService;
import com.example.usertest.domain.dto.auth.LoginRequest;
import com.example.usertest.domain.dto.auth.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@RestController
@RequestMapping("${api.prefix:/api/v1}/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "APIs for user authentication and token management")
public class AuthController {
    
    private final KeycloakTokenService keycloakTokenService;
    private final KeycloakProperties keycloakProperties;
    
    @PostMapping("/login")
    @Operation(
        summary = "User login and token issuance", 
        description = "Authenticate user with Keycloak and return JWT tokens"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Successfully authenticated and tokens issued",
            content = @Content(schema = @Schema(implementation = TokenResponse.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid login credentials",
            content = @Content(schema = @Schema(implementation = String.class))
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Authentication failed",
            content = @Content(schema = @Schema(implementation = String.class))
        ),
        @ApiResponse(
            responseCode = "503", 
            description = "Keycloak service unavailable",
            content = @Content(schema = @Schema(implementation = String.class))
        )
    })
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login attempt for user: {}", loginRequest.getUsername());
        
        try {
            TokenResponse tokenResponse = keycloakTokenService.getToken(loginRequest);
            
            log.info("User {} successfully authenticated", loginRequest.getUsername());
            return ResponseEntity.ok(tokenResponse);
            
        } catch (RuntimeException e) {
            log.warn("Authentication failed for user {}: {}", loginRequest.getUsername(), e.getMessage());
            
            if (e.getMessage().contains("Authentication failed")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Invalid username or password");
            } else if (e.getMessage().contains("service unavailable")) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("Authentication service is currently unavailable");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Login request failed: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("Unexpected error during login for user {}", loginRequest.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred");
        }
    }
    
    @PostMapping("/validate")
    @Operation(
        summary = "Validate token", 
        description = "Validate JWT token issued by Keycloak"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Token is valid",
            content = @Content(schema = @Schema(implementation = String.class))
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Token is invalid or expired",
            content = @Content(schema = @Schema(implementation = String.class))
        )
    })
    public ResponseEntity<String> validateToken(@RequestHeader("Authorization") String authorizationHeader) {
        log.info("Token validation request");
        
        try {
            // Bearer 토큰에서 실제 토큰 값 추출
            String token = extractTokenFromHeader(authorizationHeader);
            
            boolean isValid = keycloakTokenService.validateToken(token);
            
            if (isValid) {
                log.info("Token validation successful");
                return ResponseEntity.ok("Token is valid");
            } else {
                log.warn("Token validation failed - token is invalid");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Token is invalid or expired");
            }
            
        } catch (Exception e) {
            log.error("Error during token validation", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid token format");
        }
    }
    
    @GetMapping("/health")
    @Operation(
        summary = "Authentication service health check", 
        description = "Check if authentication service and Keycloak are available"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Service is healthy",
        content = @Content(schema = @Schema(implementation = String.class))
    )
    public ResponseEntity<String> healthCheck() {
        log.debug("Authentication service health check");
        return ResponseEntity.ok("Authentication service is running");
    }
    
    @GetMapping("/test-encoding")
    @Operation(
        summary = "Test Korean encoding", 
        description = "Test endpoint for Korean text encoding verification"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Encoding test response",
        content = @Content(schema = @Schema(implementation = Map.class))
    )
    public ResponseEntity<?> testEncoding() {
        log.info("한글 인코딩 테스트 요청");
        
        return ResponseEntity.ok(Map.of(
            "message", "한글 인코딩이 정상적으로 작동합니다",
            "service", "사용자 테스트 서비스",
            "status", "정상",
            "encoding", "UTF-8",
            "timestamp", System.currentTimeMillis(),
            "test_data", Map.of(
                "korean", "한글 테스트",
                "english", "English test",
                "mixed", "Mixed 혼합 텍스트 test",
                "special", "특수문자: !@#$%^&*()",
                "numbers", "숫자: 1234567890"
            )
        ));
    }
    
    @GetMapping("/test-connection")
    @Operation(
        summary = "Test Keycloak connection", 
        description = "Test connectivity to Keycloak server and realm configuration"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Connection test results",
        content = @Content(schema = @Schema(implementation = Map.class))
    )
    public ResponseEntity<?> testKeycloakConnection() {
        try {
            log.info("Testing Keycloak connection...");
            
            String serverUrl = keycloakProperties.getServerUrl();
            String realm = keycloakProperties.getRealm();
            String configEndpoint = serverUrl + "/realms/" + realm + "/.well-known/openid_configuration";
            
            WebClient webClient = WebClient.builder().build();
            
            String response = webClient
                    .get()
                    .uri(configEndpoint)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "keycloak_url", serverUrl,
                "realm", realm,
                "config_endpoint", configEndpoint,
                "response", response,
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            log.error("Keycloak connection test failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                        "status", "failed",
                        "keycloak_url", keycloakProperties.getServerUrl(),
                        "realm", keycloakProperties.getRealm(),
                        "error", e.getMessage(),
                        "error_type", e.getClass().getSimpleName(),
                        "timestamp", System.currentTimeMillis()
                    ));
        }
    }
    
    /**
     * Authorization 헤더에서 Bearer 토큰을 추출합니다.
     */
    private String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid Authorization header format");
        }
        return authorizationHeader.substring(7); // "Bearer " 제거
    }
}