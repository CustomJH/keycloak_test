package com.example.usertest.api.service.auth;

import com.example.usertest.api.config.properties.KeycloakProperties;
import com.example.usertest.domain.dto.auth.KeycloakTokenRequest;
import com.example.usertest.domain.dto.auth.LoginRequest;
import com.example.usertest.domain.dto.auth.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakTokenService {
    
    private final KeycloakProperties keycloakProperties;
    private final WebClient.Builder webClientBuilder;
    
    /**
     * Keycloak에서 토큰을 발급받습니다.
     */
    public TokenResponse getToken(LoginRequest loginRequest) {
        log.info("Requesting token from Keycloak for user: {}", loginRequest.getUsername());
        
        try {
            // Keycloak 토큰 요청 준비
            KeycloakTokenRequest tokenRequest = KeycloakTokenRequest.fromLogin(
                loginRequest, 
                keycloakProperties.getClientId(), 
                keycloakProperties.getClientSecret()
            );
            
            // Form data 준비 (Keycloak은 application/x-www-form-urlencoded 형식 요구)
            MultiValueMap<String, String> formData = createFormData(tokenRequest);
            
            // WebClient를 사용하여 Keycloak 토큰 엔드포인트 호출
            WebClient webClient = webClientBuilder
                    .baseUrl(keycloakProperties.getServerUrl())
                    .build();
            
            TokenResponse tokenResponse = webClient
                    .post()
                    .uri(getTokenEndpointPath())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), response -> {
                        log.error("Client error from Keycloak: {}", response.statusCode());
                        return response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Error body: {}", errorBody);
                                    return Mono.error(new RuntimeException("Authentication failed: " + errorBody));
                                });
                    })
                    .onStatus(status -> status.is5xxServerError(), response -> {
                        log.error("Server error from Keycloak: {}", response.statusCode());
                        return Mono.error(new RuntimeException("Keycloak server error"));
                    })
                    .bodyToMono(TokenResponse.class)
                    .block(); // 동기 방식으로 결과 반환
            
            log.info("Token successfully obtained for user: {}", loginRequest.getUsername());
            return tokenResponse;
            
        } catch (WebClientResponseException e) {
            log.error("Failed to get token from Keycloak: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to authenticate with Keycloak: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Unexpected error while getting token from Keycloak", e);
            throw new RuntimeException("Authentication service unavailable", e);
        }
    }
    
    /**
     * Keycloak 토큰 엔드포인트 경로를 생성합니다.
     */
    private String getTokenEndpointPath() {
        return String.format("/realms/%s/protocol/openid-connect/token", keycloakProperties.getRealm());
    }
    
    /**
     * Keycloak API 호출을 위한 Form Data를 생성합니다.
     */
    private MultiValueMap<String, String> createFormData(KeycloakTokenRequest tokenRequest) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", tokenRequest.getGrantType());
        formData.add("client_id", tokenRequest.getClientId());
        formData.add("client_secret", tokenRequest.getClientSecret());
        formData.add("username", tokenRequest.getUsername());
        formData.add("password", tokenRequest.getPassword());
        formData.add("scope", tokenRequest.getScope());
        
        return formData;
    }
    
    /**
     * 토큰이 유효한지 확인합니다. (필요시 구현)
     */
    public boolean validateToken(String token) {
        // TODO: Keycloak introspection endpoint를 사용하여 토큰 유효성 검증
        return true;
    }
    
    /**
     * 리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다. (필요시 구현)
     */
    public TokenResponse refreshToken(String refreshToken) {
        // TODO: Refresh token 구현
        throw new UnsupportedOperationException("Refresh token not implemented yet");
    }
}