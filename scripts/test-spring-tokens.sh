#!/bin/bash

# Spring Boot - Keycloak JWT Token Testing Script
# Tests different authentication flows for Spring Boot application integration

set -e

KEYCLOAK_URL="http://localhost:18200"
REALM="spring-app-realm"
BACKEND_CLIENT_ID="spring-backend-client"
BACKEND_CLIENT_SECRET="spring-backend-secret"
FRONTEND_CLIENT_ID="spring-frontend-client"

# Test user credentials
USER1="user1"
USER1_PASSWORD="user123!"
ADMIN1="admin1"
ADMIN1_PASSWORD="admin123!"
MANAGER1="manager1"
MANAGER1_PASSWORD="manager123!"

echo "🔐 Spring Boot - Keycloak JWT Token Testing"
echo "============================================"

# Function to decode JWT (header and payload)
decode_jwt() {
    local token=$1
    echo "📋 JWT Token Analysis:"
    
    # Extract and decode header
    local header=$(echo $token | cut -d'.' -f1)
    local padded_header="${header}$(printf '%0.s' '=' {1..4})"
    echo "Header:"
    echo $padded_header | base64 -D 2>/dev/null | jq . 2>/dev/null || echo "Could not decode header"
    
    # Extract and decode payload
    local payload=$(echo $token | cut -d'.' -f2)
    local padded_payload="${payload}$(printf '%0.s' '=' {1..4})"
    echo ""
    echo "Payload:"
    echo $padded_payload | base64 -D 2>/dev/null | jq . 2>/dev/null || echo "Could not decode payload"
    
    echo ""
}

# Test 1: Backend Service Authentication (Client Credentials Flow)
echo "🚀 Test 1: Backend Service Authentication (Client Credentials)"
echo "------------------------------------------------------------"
echo "For Spring Boot backend-to-backend communication."

BACKEND_TOKEN_RESPONSE=$(curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=client_credentials" \
    -d "client_id=$BACKEND_CLIENT_ID" \
    -d "client_secret=$BACKEND_CLIENT_SECRET")

BACKEND_ACCESS_TOKEN=$(echo $BACKEND_TOKEN_RESPONSE | jq -r '.access_token')

if [ "$BACKEND_ACCESS_TOKEN" != "null" ] && [ -n "$BACKEND_ACCESS_TOKEN" ]; then
    echo "✅ Backend service token obtained successfully"
    echo "Token Type: $(echo $BACKEND_TOKEN_RESPONSE | jq -r '.token_type')"
    echo "Expires In: $(echo $BACKEND_TOKEN_RESPONSE | jq -r '.expires_in') seconds"
    decode_jwt "$BACKEND_ACCESS_TOKEN"
    echo "$BACKEND_ACCESS_TOKEN" > /tmp/spring-backend-token.jwt
    echo "💾 Backend token saved to /tmp/spring-backend-token.jwt"
else
    echo "❌ Failed to get backend service token"
    echo "Response: $BACKEND_TOKEN_RESPONSE"
fi

echo ""
echo "🚀 Test 2: User Authentication with Different Roles"
echo "--------------------------------------------------"

# Test USER role authentication
echo "👤 Testing USER role (user1):"
USER_TOKEN_RESPONSE=$(curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=password" \
    -d "client_id=$FRONTEND_CLIENT_ID" \
    -d "username=$USER1" \
    -d "password=$USER1_PASSWORD")

USER_ACCESS_TOKEN=$(echo $USER_TOKEN_RESPONSE | jq -r '.access_token')

if [ "$USER_ACCESS_TOKEN" != "null" ] && [ -n "$USER_ACCESS_TOKEN" ]; then
    echo "✅ USER token obtained successfully"
    echo "Username: $USER1"
    echo "Token Type: $(echo $USER_TOKEN_RESPONSE | jq -r '.token_type')"
    echo "Expires In: $(echo $USER_TOKEN_RESPONSE | jq -r '.expires_in') seconds"
    echo "Has Refresh Token: $([ "$(echo $USER_TOKEN_RESPONSE | jq -r '.refresh_token')" != "null" ] && echo "Yes" || echo "No")"
    decode_jwt "$USER_ACCESS_TOKEN"
    echo "$USER_ACCESS_TOKEN" > /tmp/spring-user-token.jwt
    echo "💾 User token saved to /tmp/spring-user-token.jwt"
else
    echo "❌ Failed to get USER token"
    echo "Response: $USER_TOKEN_RESPONSE"
fi

echo ""
echo "👨‍💼 Testing ADMIN role (admin1):"
ADMIN_TOKEN_RESPONSE=$(curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=password" \
    -d "client_id=$FRONTEND_CLIENT_ID" \
    -d "username=$ADMIN1" \
    -d "password=$ADMIN1_PASSWORD")

ADMIN_ACCESS_TOKEN=$(echo $ADMIN_TOKEN_RESPONSE | jq -r '.access_token')

if [ "$ADMIN_ACCESS_TOKEN" != "null" ] && [ -n "$ADMIN_ACCESS_TOKEN" ]; then
    echo "✅ ADMIN token obtained successfully"
    echo "Username: $ADMIN1"
    echo "Token Type: $(echo $ADMIN_TOKEN_RESPONSE | jq -r '.token_type')"
    decode_jwt "$ADMIN_ACCESS_TOKEN"
    echo "$ADMIN_ACCESS_TOKEN" > /tmp/spring-admin-token.jwt
    echo "💾 Admin token saved to /tmp/spring-admin-token.jwt"
else
    echo "❌ Failed to get ADMIN token"
    echo "Response: $ADMIN_TOKEN_RESPONSE"
fi

echo ""
echo "👔 Testing MANAGER role (manager1):"
MANAGER_TOKEN_RESPONSE=$(curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=password" \
    -d "client_id=$FRONTEND_CLIENT_ID" \
    -d "username=$MANAGER1" \
    -d "password=$MANAGER1_PASSWORD")

MANAGER_ACCESS_TOKEN=$(echo $MANAGER_TOKEN_RESPONSE | jq -r '.access_token')

if [ "$MANAGER_ACCESS_TOKEN" != "null" ] && [ -n "$MANAGER_ACCESS_TOKEN" ]; then
    echo "✅ MANAGER token obtained successfully"
    echo "Username: $MANAGER1"
    echo "Token Type: $(echo $MANAGER_TOKEN_RESPONSE | jq -r '.token_type')"
    decode_jwt "$MANAGER_ACCESS_TOKEN"
    echo "$MANAGER_ACCESS_TOKEN" > /tmp/spring-manager-token.jwt
    echo "💾 Manager token saved to /tmp/spring-manager-token.jwt"
else
    echo "❌ Failed to get MANAGER token"
    echo "Response: $MANAGER_TOKEN_RESPONSE"
fi

echo ""
echo "🚀 Test 3: Token Validation & Introspection"
echo "------------------------------------------"

if [ "$USER_ACCESS_TOKEN" != "null" ] && [ -n "$USER_ACCESS_TOKEN" ]; then
    INTROSPECTION_RESPONSE=$(curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token/introspect" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "token=$USER_ACCESS_TOKEN" \
        -d "client_id=$FRONTEND_CLIENT_ID")
    
    IS_ACTIVE=$(echo $INTROSPECTION_RESPONSE | jq -r '.active')
    
    if [ "$IS_ACTIVE" = "true" ]; then
        echo "✅ User token is valid and active"
        echo "Subject: $(echo $INTROSPECTION_RESPONSE | jq -r '.sub')"
        echo "Username: $(echo $INTROSPECTION_RESPONSE | jq -r '.preferred_username')"
        echo "Client ID: $(echo $INTROSPECTION_RESPONSE | jq -r '.client_id')"
        echo "Scope: $(echo $INTROSPECTION_RESPONSE | jq -r '.scope')"
    else
        echo "❌ User token validation failed"
    fi
fi

echo ""
echo "🚀 Test 4: OIDC Discovery & Public Keys"
echo "--------------------------------------"

DISCOVERY_RESPONSE=$(curl -s "$KEYCLOAK_URL/realms/$REALM/.well-known/openid_configuration")
ISSUER=$(echo $DISCOVERY_RESPONSE | jq -r '.issuer')
JWKS_URI=$(echo $DISCOVERY_RESPONSE | jq -r '.jwks_uri')
TOKEN_ENDPOINT=$(echo $DISCOVERY_RESPONSE | jq -r '.token_endpoint')
AUTH_ENDPOINT=$(echo $DISCOVERY_RESPONSE | jq -r '.authorization_endpoint')

echo "✅ OIDC Discovery Information:"
echo "Issuer: $ISSUER"
echo "JWKS URI: $JWKS_URI"
echo "Token Endpoint: $TOKEN_ENDPOINT"
echo "Authorization Endpoint: $AUTH_ENDPOINT"

echo ""
echo "🔑 Public Key Information:"
JWKS_RESPONSE=$(curl -s "$JWKS_URI")
KEY_COUNT=$(echo $JWKS_RESPONSE | jq '.keys | length')
echo "Available Keys: $KEY_COUNT"
echo "Key ID: $(echo $JWKS_RESPONSE | jq -r '.keys[0].kid')"
echo "Algorithm: $(echo $JWKS_RESPONSE | jq -r '.keys[0].alg')"

echo ""
echo "🎯 Spring Boot Configuration Guide"
echo "=================================="
echo ""
echo "📋 Maven Dependencies (pom.xml):"
echo "<dependency>"
echo "    <groupId>org.springframework.boot</groupId>"
echo "    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>"
echo "</dependency>"
echo "<dependency>"
echo "    <groupId>org.springframework.boot</groupId>"
echo "    <artifactId>spring-boot-starter-oauth2-client</artifactId>"
echo "</dependency>"
echo ""
echo "📋 Application Configuration (application.yml):"
echo "spring:"
echo "  security:"
echo "    oauth2:"
echo "      resourceserver:"
echo "        jwt:"
echo "          issuer-uri: $ISSUER"
echo "          jwk-set-uri: $JWKS_URI"
echo "      client:"
echo "        registration:"
echo "          keycloak:"
echo "            client-id: $FRONTEND_CLIENT_ID"
echo "            client-secret: (not needed for public client)"
echo "            authorization-grant-type: authorization_code"
echo "            redirect-uri: \"{baseUrl}/login/oauth2/code/{registrationId}\""
echo "            scope: openid,profile,email"
echo "        provider:"
echo "          keycloak:"
echo "            issuer-uri: $ISSUER"
echo "            authorization-uri: $AUTH_ENDPOINT"
echo "            token-uri: $TOKEN_ENDPOINT"
echo "            jwk-set-uri: $JWKS_URI"
echo "            user-info-uri: $KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/userinfo"
echo "            user-name-attribute: preferred_username"
echo ""
echo "📋 Security Configuration Example:"
echo "@Configuration"
echo "@EnableWebSecurity"
echo "public class SecurityConfig {"
echo ""
echo "    @Bean"
echo "    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {"
echo "        http"
echo "            .authorizeHttpRequests(authorize -> authorize"
echo "                .requestMatchers(\"/api/public/**\").permitAll()"
echo "                .requestMatchers(\"/api/user/**\").hasRole(\"USER\")"
echo "                .requestMatchers(\"/api/admin/**\").hasRole(\"ADMIN\")"
echo "                .requestMatchers(\"/api/manager/**\").hasRole(\"MANAGER\")"
echo "                .anyRequest().authenticated()"
echo "            )"
echo "            .oauth2ResourceServer(oauth2 -> oauth2"
echo "                .jwt(jwt -> jwt"
echo "                    .jwtAuthenticationConverter(jwtAuthenticationConverter())"
echo "                )"
echo "            );"
echo "        return http.build();"
echo "    }"
echo ""
echo "    @Bean"
echo "    public JwtAuthenticationConverter jwtAuthenticationConverter() {"
echo "        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();"
echo "        authoritiesConverter.setAuthorityPrefix(\"ROLE_\");"
echo "        authoritiesConverter.setAuthoritiesClaimName(\"realm_access.roles\");"
echo ""
echo "        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();"
echo "        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);"
echo "        return converter;"
echo "    }"
echo "}"
echo ""
echo "📋 Controller Example:"
echo "@RestController"
echo "@RequestMapping(\"/api\")"
echo "public class TestController {"
echo ""
echo "    @GetMapping(\"/public/hello\")"
echo "    public String publicEndpoint() {"
echo "        return \"Hello from public endpoint!\";"
echo "    }"
echo ""
echo "    @GetMapping(\"/user/profile\")"
echo "    @PreAuthorize(\"hasRole('USER')\")"
echo "    public String userEndpoint(Authentication auth) {"
echo "        return \"Hello \" + auth.getName() + \" from user endpoint!\";"
echo "    }"
echo ""
echo "    @GetMapping(\"/admin/dashboard\")"
echo "    @PreAuthorize(\"hasRole('ADMIN')\")"
echo "    public String adminEndpoint(Authentication auth) {"
echo "        return \"Hello \" + auth.getName() + \" from admin endpoint!\";"
echo "    }"
echo "}"
echo ""
echo "📋 Testing with curl:"
echo "# Test public endpoint (no auth required)"
echo "curl http://localhost:8080/api/public/hello"
echo ""
echo "# Test user endpoint with token"
echo "curl -H \"Authorization: Bearer \$(cat /tmp/spring-user-token.jwt)\" \\"
echo "     http://localhost:8080/api/user/profile"
echo ""
echo "# Test admin endpoint with admin token"
echo "curl -H \"Authorization: Bearer \$(cat /tmp/spring-admin-token.jwt)\" \\"
echo "     http://localhost:8080/api/admin/dashboard"
echo ""
echo "✅ Spring Boot - Keycloak integration testing completed!"
echo ""
echo "💾 Generated token files:"
echo "- Backend service token: /tmp/spring-backend-token.jwt"
echo "- User token: /tmp/spring-user-token.jwt"
echo "- Admin token: /tmp/spring-admin-token.jwt"
echo "- Manager token: /tmp/spring-manager-token.jwt"