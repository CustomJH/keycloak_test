# Spring Boot + Keycloak JWT 인증 통합 완벽 가이드

Spring Boot 애플리케이션에서 Keycloak을 사용한 JWT 토큰 기반 인증 구현을 위한 완전한 설정 가이드입니다.

## ✅ **완료된 Keycloak 설정**

### 생성된 리소스
- **Realm**: `spring-app-realm`
- **Backend Client**: `spring-backend-client` (Confidential)
- **Frontend Client**: `spring-frontend-client` (Public)
- **테스트 사용자들**: 
  - `user1` / `user123!` (USER 역할)
  - `admin1` / `admin123!` (ADMIN, USER 역할)
  - `manager1` / `manager123!` (MANAGER, USER 역할)

### 주요 엔드포인트
- **OIDC Discovery**: http://localhost:18200/realms/spring-app-realm/.well-known/openid_configuration
- **Token Endpoint**: http://localhost:18200/realms/spring-app-realm/protocol/openid-connect/token
- **JWKS URI**: http://localhost:18200/realms/spring-app-realm/protocol/openid-connect/certs

---

## 🚀 **Spring Boot 프로젝트 설정**

### 1. Maven 의존성 추가 (pom.xml)

```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    
    <!-- OAuth2 Resource Server (JWT 검증용) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
    </dependency>
    
    <!-- OAuth2 Client (선택사항 - 웹 로그인 필요시) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-client</artifactId>
    </dependency>
</dependencies>
```

### 2. Application 설정 (application.yml)

```yaml
server:
  port: 8080

spring:
  application:
    name: spring-boot-keycloak-app
    
  security:
    oauth2:
      # JWT Resource Server 설정 (API 인증용)
      resourceserver:
        jwt:
          issuer-uri: http://localhost:18200/realms/spring-app-realm
          jwk-set-uri: http://localhost:18200/realms/spring-app-realm/protocol/openid-connect/certs
          
      # OAuth2 Client 설정 (웹 로그인용, 선택사항)
      client:
        registration:
          keycloak:
            client-id: spring-frontend-client
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope: openid,profile,email
        provider:
          keycloak:
            issuer-uri: http://localhost:18200/realms/spring-app-realm

# 로깅 설정
logging:
  level:
    org.springframework.security: DEBUG
    org.springframework.security.oauth2: DEBUG
```

### 3. Security Configuration

```java
package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // Public endpoints
                .requestMatchers("/api/public/**", "/health", "/actuator/**").permitAll()
                // Role-based access
                .requestMatchers("/api/user/**").hasRole("USER")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/manager/**").hasRole("MANAGER")
                // All other requests need authentication
                .anyRequest().authenticated()
            )
            // JWT Resource Server configuration
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            // Disable CSRF for API
            .csrf(csrf -> csrf.disable());
            
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        return converter;
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        // Extract roles from realm_access.roles
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        Collection<String> roles = (Collection<String>) realmAccess.get("roles");
        
        return roles.stream()
            .filter(role -> !role.startsWith("default-roles-") && 
                           !role.equals("offline_access") && 
                           !role.equals("uma_authorization"))
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toList());
    }
}
```

### 4. REST Controller 예제

```java
package com.example.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/public/hello")
    public Map<String, String> publicEndpoint() {
        return Map.of("message", "Hello from public endpoint!", "status", "success");
    }

    @GetMapping("/user/profile")
    @PreAuthorize("hasRole('USER')")
    public Map<String, Object> userEndpoint(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        
        return Map.of(
            "message", "Hello from user endpoint!",
            "user", jwt.getClaimAsString("preferred_username"),
            "roles", authentication.getAuthorities(),
            "status", "success"
        );
    }

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> adminEndpoint(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        
        return Map.of(
            "message", "Hello from admin dashboard!",
            "admin", jwt.getClaimAsString("preferred_username"),
            "email", jwt.getClaimAsString("email"),
            "roles", authentication.getAuthorities(),
            "status", "success"
        );
    }

    @GetMapping("/manager/reports")
    @PreAuthorize("hasRole('MANAGER')")
    public Map<String, Object> managerEndpoint(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        
        return Map.of(
            "message", "Hello from manager reports!",
            "manager", jwt.getClaimAsString("preferred_username"),
            "roles", authentication.getAuthorities(),
            "status", "success"
        );
    }

    @GetMapping("/protected/info")
    public Map<String, Object> protectedEndpoint(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        
        return Map.of(
            "message", "This is a protected endpoint",
            "user", jwt.getClaimAsString("preferred_username"),
            "subject", jwt.getSubject(),
            "issuer", jwt.getIssuer().toString(),
            "audience", jwt.getAudience(),
            "expiresAt", jwt.getExpiresAt(),
            "roles", authentication.getAuthorities()
        );
    }
}
```

---

## 🧪 **애플리케이션 테스트**

### 1. Spring Boot 애플리케이션 실행
```bash
./mvnw spring-boot:run
```

### 2. 토큰 발급 및 API 테스트

```bash
# 1. USER 토큰으로 사용자 API 테스트
USER_TOKEN=$(cat /tmp/spring-user-token.jwt)
curl -H "Authorization: Bearer $USER_TOKEN" \
     http://localhost:8080/api/user/profile

# 2. ADMIN 토큰으로 관리자 API 테스트
ADMIN_TOKEN=$(cat /tmp/spring-admin-token.jwt)
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
     http://localhost:8080/api/admin/dashboard

# 3. MANAGER 토큰으로 매니저 API 테스트  
MANAGER_TOKEN=$(cat /tmp/spring-manager-token.jwt)
curl -H "Authorization: Bearer $MANAGER_TOKEN" \
     http://localhost:8080/api/manager/reports

# 4. 백엔드 서비스 토큰으로 보호된 API 테스트
BACKEND_TOKEN=$(cat /tmp/spring-backend-token.jwt)
curl -H "Authorization: Bearer $BACKEND_TOKEN" \
     http://localhost:8080/api/protected/info

# 5. Public API 테스트 (토큰 없이)
curl http://localhost:8080/api/public/hello
```

### 3. 예상 응답 예제

```json
// USER API Response
{
  "message": "Hello from user endpoint!",
  "user": "user1",
  "roles": ["ROLE_USER"],
  "status": "success"
}

// ADMIN API Response  
{
  "message": "Hello from admin dashboard!",
  "admin": "admin1",
  "email": "admin1@example.com",
  "roles": ["ROLE_ADMIN", "ROLE_USER"],
  "status": "success"
}
```

---

## 🔄 **실제 사용 시나리오**

### 1. Frontend (React/Vue/Angular)에서 토큰 사용

```javascript
// 토큰 획득 (로그인)
const loginUser = async (username, password) => {
  const response = await fetch('http://localhost:18200/realms/spring-app-realm/protocol/openid-connect/token', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: new URLSearchParams({
      grant_type: 'password',
      client_id: 'spring-frontend-client',
      username: username,
      password: password,
    }),
  });
  
  const data = await response.json();
  localStorage.setItem('access_token', data.access_token);
  return data;
};

// API 호출 시 토큰 사용
const callProtectedAPI = async () => {
  const token = localStorage.getItem('access_token');
  
  const response = await fetch('http://localhost:8080/api/user/profile', {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  });
  
  return response.json();
};
```

### 2. 서비스 간 통신 (Backend-to-Backend)

```java
@Service
public class ExternalApiService {
    
    @Value("${keycloak.token-uri}")
    private String tokenUri;
    
    @Value("${keycloak.client-id}")
    private String clientId;
    
    @Value("${keycloak.client-secret}")
    private String clientSecret;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    public String getServiceToken() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUri, request, Map.class);
        return (String) response.getBody().get("access_token");
    }
    
    public String callExternalService() {
        String token = getServiceToken();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
            "http://other-service/api/data",
            HttpMethod.GET,
            entity,
            String.class
        );
        
        return response.getBody();
    }
}
```

---

## 🛡️ **보안 고려사항**

### 1. 프로덕션 설정
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          # HTTPS 사용
          issuer-uri: https://your-keycloak-domain/realms/spring-app-realm
          jwk-set-uri: https://your-keycloak-domain/realms/spring-app-realm/protocol/openid-connect/certs
          
# 토큰 캐싱 설정
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

### 2. 에러 처리
```java
@ControllerAdvice
public class SecurityExceptionHandler {
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of("error", "Access denied", "message", "Insufficient privileges"));
    }
    
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<Map<String, String>> handleJwtException(JwtException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", "Invalid token", "message", ex.getMessage()));
    }
}
```

---

## ✅ **완료 체크리스트**

- [x] Keycloak realm 및 clients 설정 완료
- [x] 테스트 사용자 및 역할 생성 완료  
- [x] JWT 토큰 발급 테스트 완료
- [x] Spring Boot 의존성 설정
- [x] Security Configuration 구현
- [x] REST Controller with Role-based Access
- [x] Token validation 테스트
- [x] API 엔드포인트 테스트

---

## 🎯 **즉시 사용 가능한 명령어들**

```bash
# 모든 토큰 재생성
./scripts/test-spring-tokens.sh

# 특정 역할 토큰으로 API 테스트
curl -H "Authorization: Bearer $(cat /tmp/spring-user-token.jwt)" \
     http://localhost:8080/api/user/profile

curl -H "Authorization: Bearer $(cat /tmp/spring-admin-token.jwt)" \
     http://localhost:8080/api/admin/dashboard
```

**이제 Spring Boot 애플리케이션에서 Keycloak JWT 인증을 완전하게 사용할 수 있습니다!** 🎉