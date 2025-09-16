#!/bin/bash

# Spring Boot - Keycloak Integration Setup Script
# Creates realm, clients, users, and roles for Spring Boot application

set -e

KEYCLOAK_URL="http://localhost:18200"
ADMIN_USER="admin"
ADMIN_PASSWORD="admin!34"

echo "🚀 Setting up Spring Boot - Keycloak Integration"
echo "================================================"

# Get admin token
echo "📋 Getting admin access token..."
ADMIN_TOKEN=$(curl -s -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "username=$ADMIN_USER" \
    -d "password=$ADMIN_PASSWORD" \
    -d "grant_type=password" \
    -d "client_id=admin-cli" | jq -r '.access_token')

if [ "$ADMIN_TOKEN" == "null" ] || [ -z "$ADMIN_TOKEN" ]; then
    echo "❌ Failed to get admin token"
    exit 1
fi
echo "✅ Admin token obtained"

# Create Spring realm
echo "📋 Creating spring-app-realm..."
curl -s -X POST "$KEYCLOAK_URL/admin/realms" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
        "realm": "spring-app-realm",
        "displayName": "Spring Boot Application Realm",
        "enabled": true,
        "accessTokenLifespan": 3600,
        "refreshTokenMaxReuse": 0,
        "ssoSessionMaxLifespan": 36000,
        "accessTokenLifespanForImplicitFlow": 900,
        "defaultSignatureAlgorithm": "RS256",
        "revokeRefreshToken": false,
        "refreshTokenMaxReuse": 0,
        "browserFlow": "browser",
        "registrationFlow": "registration",
        "directGrantFlow": "direct grant",
        "resetCredentialsFlow": "reset credentials",
        "clientAuthenticationFlow": "clients",
        "dockerAuthenticationFlow": "docker auth",
        "attributes": {
            "frontendUrl": "",
            "requiresPushedAuthorizationRequests": "false"
        }
    }' || echo "⚠️  Realm might already exist"

echo "✅ Spring realm created/verified"

# Create Spring Backend Client (Resource Server)
echo "📋 Creating spring-backend-client..."
curl -s -X POST "$KEYCLOAK_URL/admin/realms/spring-app-realm/clients" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
        "clientId": "spring-backend-client",
        "name": "Spring Boot Backend Application",
        "description": "Resource server for Spring Boot backend API",
        "enabled": true,
        "clientAuthenticatorType": "client-secret",
        "secret": "spring-backend-secret",
        "standardFlowEnabled": true,
        "implicitFlowEnabled": false,
        "directAccessGrantsEnabled": true,
        "serviceAccountsEnabled": true,
        "publicClient": false,
        "bearerOnly": true,
        "protocol": "openid-connect",
        "attributes": {
            "access.token.lifespan": "3600",
            "client.secret.creation.time": "1694764800",
            "oauth2.device.authorization.grant.enabled": "false",
            "oidc.ciba.grant.enabled": "false",
            "backchannel.logout.session.required": "true",
            "backchannel.logout.revoke.offline.tokens": "false"
        },
        "authorizationServicesEnabled": true,
        "directAccessGrantsEnabled": true,
        "serviceAccountsEnabled": true,
        "fullScopeAllowed": true
    }' || echo "⚠️  Backend client might already exist"

echo "✅ Spring backend client created/verified"

# Create Spring Frontend Client (Public Client for SPA/React/Vue)
echo "📋 Creating spring-frontend-client..."
curl -s -X POST "$KEYCLOAK_URL/admin/realms/spring-app-realm/clients" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
        "clientId": "spring-frontend-client",
        "name": "Spring Boot Frontend Application",
        "description": "Public client for frontend applications (React, Vue, Angular)",
        "enabled": true,
        "clientAuthenticatorType": "client-secret",
        "standardFlowEnabled": true,
        "implicitFlowEnabled": false,
        "directAccessGrantsEnabled": true,
        "serviceAccountsEnabled": false,
        "publicClient": true,
        "protocol": "openid-connect",
        "redirectUris": [
            "http://localhost:3000/*",
            "http://localhost:8080/*",
            "http://localhost:4200/*"
        ],
        "webOrigins": [
            "http://localhost:3000",
            "http://localhost:8080",
            "http://localhost:4200"
        ],
        "attributes": {
            "pkce.code.challenge.method": "S256",
            "post.logout.redirect.uris": "http://localhost:3000/logout"
        },
        "directAccessGrantsEnabled": true,
        "fullScopeAllowed": true
    }' || echo "⚠️  Frontend client might already exist"

echo "✅ Spring frontend client created/verified"

# Create application roles
echo "📋 Creating application roles..."

# USER role
curl -s -X POST "$KEYCLOAK_URL/admin/realms/spring-app-realm/roles" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
        "name": "USER",
        "description": "Standard application user role",
        "composite": false,
        "clientRole": false
    }' || echo "⚠️  USER role might already exist"

# ADMIN role
curl -s -X POST "$KEYCLOAK_URL/admin/realms/spring-app-realm/roles" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
        "name": "ADMIN",
        "description": "Application administrator role",
        "composite": false,
        "clientRole": false
    }' || echo "⚠️  ADMIN role might already exist"

# MANAGER role
curl -s -X POST "$KEYCLOAK_URL/admin/realms/spring-app-realm/roles" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
        "name": "MANAGER",
        "description": "Application manager role",
        "composite": false,
        "clientRole": false
    }' || echo "⚠️  MANAGER role might already exist"

echo "✅ Application roles created/verified"

# Create test users
echo "📋 Creating test users..."

# Regular user
curl -s -X POST "$KEYCLOAK_URL/admin/realms/spring-app-realm/users" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
        "username": "user1",
        "enabled": true,
        "firstName": "John",
        "lastName": "User",
        "email": "user1@example.com",
        "emailVerified": true,
        "credentials": [{
            "type": "password",
            "value": "user123!",
            "temporary": false
        }]
    }' || echo "⚠️  user1 might already exist"

# Admin user
curl -s -X POST "$KEYCLOAK_URL/admin/realms/spring-app-realm/users" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
        "username": "admin1",
        "enabled": true,
        "firstName": "Admin",
        "lastName": "User",
        "email": "admin1@example.com",
        "emailVerified": true,
        "credentials": [{
            "type": "password",
            "value": "admin123!",
            "temporary": false
        }]
    }' || echo "⚠️  admin1 might already exist"

# Manager user
curl -s -X POST "$KEYCLOAK_URL/admin/realms/spring-app-realm/users" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
        "username": "manager1",
        "enabled": true,
        "firstName": "Manager",
        "lastName": "User",
        "email": "manager1@example.com",
        "emailVerified": true,
        "credentials": [{
            "type": "password",
            "value": "manager123!",
            "temporary": false
        }]
    }' || echo "⚠️  manager1 might already exist"

echo "✅ Test users created/verified"

# Assign roles to users
echo "📋 Assigning roles to users..."

# Get user IDs
USER1_ID=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/spring-app-realm/users?username=user1" \
    -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[0].id')

ADMIN1_ID=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/spring-app-realm/users?username=admin1" \
    -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[0].id')

MANAGER1_ID=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/spring-app-realm/users?username=manager1" \
    -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[0].id')

# Get role IDs
USER_ROLE_ID=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/spring-app-realm/roles/USER" \
    -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.id')

ADMIN_ROLE_ID=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/spring-app-realm/roles/ADMIN" \
    -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.id')

MANAGER_ROLE_ID=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/spring-app-realm/roles/MANAGER" \
    -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.id')

# Assign USER role to user1
if [ "$USER1_ID" != "null" ] && [ "$USER_ROLE_ID" != "null" ]; then
    curl -s -X POST "$KEYCLOAK_URL/admin/realms/spring-app-realm/users/$USER1_ID/role-mappings/realm" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json" \
        -d "[{\"id\": \"$USER_ROLE_ID\", \"name\": \"USER\"}]"
fi

# Assign ADMIN role to admin1 (also gets USER role)
if [ "$ADMIN1_ID" != "null" ] && [ "$ADMIN_ROLE_ID" != "null" ]; then
    curl -s -X POST "$KEYCLOAK_URL/admin/realms/spring-app-realm/users/$ADMIN1_ID/role-mappings/realm" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json" \
        -d "[{\"id\": \"$ADMIN_ROLE_ID\", \"name\": \"ADMIN\"}, {\"id\": \"$USER_ROLE_ID\", \"name\": \"USER\"}]"
fi

# Assign MANAGER role to manager1 (also gets USER role)
if [ "$MANAGER1_ID" != "null" ] && [ "$MANAGER_ROLE_ID" != "null" ]; then
    curl -s -X POST "$KEYCLOAK_URL/admin/realms/spring-app-realm/users/$MANAGER1_ID/role-mappings/realm" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json" \
        -d "[{\"id\": \"$MANAGER_ROLE_ID\", \"name\": \"MANAGER\"}, {\"id\": \"$USER_ROLE_ID\", \"name\": \"USER\"}]"
fi

echo "✅ Roles assigned to users"

echo ""
echo "🎯 Spring Boot Setup Complete! Configuration Summary:"
echo "===================================================="
echo "Realm: spring-app-realm"
echo ""
echo "🔐 Clients:"
echo "  Backend (Resource Server): spring-backend-client"
echo "  Client Secret: spring-backend-secret"
echo "  Frontend (Public): spring-frontend-client"
echo ""
echo "👥 Test Users:"
echo "  Regular User: user1 / user123! (Role: USER)"
echo "  Admin User: admin1 / admin123! (Roles: ADMIN, USER)" 
echo "  Manager User: manager1 / manager123! (Roles: MANAGER, USER)"
echo ""
echo "🎭 Available Roles:"
echo "  - USER: Standard application user"
echo "  - ADMIN: Application administrator"
echo "  - MANAGER: Application manager"
echo ""
echo "🔗 Key URLs:"
echo "Admin Console: $KEYCLOAK_URL/admin"
echo "Spring Realm: $KEYCLOAK_URL/admin/master/console/#/spring-app-realm"
echo "OIDC Discovery: $KEYCLOAK_URL/realms/spring-app-realm/.well-known/openid_configuration"
echo "Token Endpoint: $KEYCLOAK_URL/realms/spring-app-realm/protocol/openid-connect/token"
echo "JWKS URI: $KEYCLOAK_URL/realms/spring-app-realm/protocol/openid-connect/certs"
echo ""
echo "🔑 Next Steps:"
echo "1. Test token generation: ./scripts/test-spring-tokens.sh"
echo "2. Configure Spring Security in your application"
echo "3. Add Keycloak dependency to your Spring Boot project"
echo "4. Configure application.yml with Keycloak settings"