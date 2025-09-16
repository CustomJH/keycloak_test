#!/bin/bash

# Pulsar-Keycloak Integration Setup Script
# Creates realm, client, users, and roles for Apache Pulsar JWT authentication

set -e

KEYCLOAK_URL="http://localhost:18200"
ADMIN_USER="admin"
ADMIN_PASSWORD="admin!34"

echo "🚀 Setting up Keycloak-Pulsar Integration"
echo "========================================"

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

# Create Pulsar realm
echo "📋 Creating pulsar-realm..."
curl -s -X POST "$KEYCLOAK_URL/admin/realms" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
        "realm": "pulsar-realm",
        "displayName": "Apache Pulsar Authentication Realm",
        "enabled": true,
        "accessTokenLifespan": 3600,
        "refreshTokenMaxReuse": 0,
        "ssoSessionMaxLifespan": 36000,
        "accessTokenLifespanForImplicitFlow": 900,
        "defaultSignatureAlgorithm": "RS256",
        "revokeRefreshToken": false,
        "refreshTokenMaxReuse": 0,
        "attributes": {
            "frontendUrl": "",
            "requiresPushedAuthorizationRequests": "false"
        }
    }' || echo "⚠️  Realm might already exist"

echo "✅ Pulsar realm created/verified"

# Create Pulsar client for service-to-service communication
echo "📋 Creating pulsar-client..."
curl -s -X POST "$KEYCLOAK_URL/admin/realms/pulsar-realm/clients" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
        "clientId": "pulsar-client",
        "name": "Apache Pulsar Service Client",
        "description": "Client for Pulsar service-to-service authentication",
        "enabled": true,
        "clientAuthenticatorType": "client-secret",
        "secret": "pulsar-client-secret",
        "standardFlowEnabled": false,
        "implicitFlowEnabled": false,
        "directAccessGrantsEnabled": true,
        "serviceAccountsEnabled": true,
        "publicClient": false,
        "protocol": "openid-connect",
        "attributes": {
            "access.token.lifespan": "3600",
            "client.secret.creation.time": "1694764800",
            "oauth2.device.authorization.grant.enabled": "false",
            "oidc.ciba.grant.enabled": "false",
            "backchannel.logout.session.required": "true",
            "backchannel.logout.revoke.offline.tokens": "false"
        },
        "authorizationServicesEnabled": false,
        "directAccessGrantsEnabled": true,
        "serviceAccountsEnabled": true
    }' || echo "⚠️  Client might already exist"

echo "✅ Pulsar client created/verified"

# Create producer role
echo "📋 Creating pulsar-producer role..."
curl -s -X POST "$KEYCLOAK_URL/admin/realms/pulsar-realm/roles" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
        "name": "pulsar-producer",
        "description": "Role for Pulsar message producers",
        "composite": false,
        "clientRole": false
    }' || echo "⚠️  Role might already exist"

# Create consumer role
echo "📋 Creating pulsar-consumer role..."
curl -s -X POST "$KEYCLOAK_URL/admin/realms/pulsar-realm/roles" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
        "name": "pulsar-consumer",
        "description": "Role for Pulsar message consumers", 
        "composite": false,
        "clientRole": false
    }' || echo "⚠️  Role might already exist"

# Create admin role
echo "📋 Creating pulsar-admin role..."
curl -s -X POST "$KEYCLOAK_URL/admin/realms/pulsar-realm/roles" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
        "name": "pulsar-admin",
        "description": "Role for Pulsar administrators",
        "composite": false,
        "clientRole": false
    }' || echo "⚠️  Role might already exist"

echo "✅ Pulsar roles created/verified"

# Create test user for Pulsar
echo "📋 Creating pulsar test user..."
curl -s -X POST "$KEYCLOAK_URL/admin/realms/pulsar-realm/users" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
        "username": "pulsar-user",
        "enabled": true,
        "firstName": "Pulsar",
        "lastName": "User",
        "email": "pulsar-user@example.com",
        "credentials": [{
            "type": "password",
            "value": "pulsar123!",
            "temporary": false
        }]
    }' || echo "⚠️  User might already exist"

echo "✅ Pulsar test user created/verified"

# Get user ID and assign roles
echo "📋 Assigning roles to pulsar-user..."
USER_ID=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/pulsar-realm/users?username=pulsar-user" \
    -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[0].id')

if [ "$USER_ID" != "null" ] && [ -n "$USER_ID" ]; then
    # Get role IDs
    PRODUCER_ROLE_ID=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/pulsar-realm/roles/pulsar-producer" \
        -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.id')
    
    CONSUMER_ROLE_ID=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/pulsar-realm/roles/pulsar-consumer" \
        -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.id')
    
    # Assign producer role
    curl -s -X POST "$KEYCLOAK_URL/admin/realms/pulsar-realm/users/$USER_ID/role-mappings/realm" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json" \
        -d "[{\"id\": \"$PRODUCER_ROLE_ID\", \"name\": \"pulsar-producer\"}]" || echo "⚠️  Role assignment might have failed"
    
    # Assign consumer role
    curl -s -X POST "$KEYCLOAK_URL/admin/realms/pulsar-realm/users/$USER_ID/role-mappings/realm" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json" \
        -d "[{\"id\": \"$CONSUMER_ROLE_ID\", \"name\": \"pulsar-consumer\"}]" || echo "⚠️  Role assignment might have failed"
    
    echo "✅ Roles assigned to pulsar-user"
else
    echo "⚠️  Could not find user ID for role assignment"
fi

echo ""
echo "🎯 Setup Complete! Configuration Summary:"
echo "=========================================="
echo "Realm: pulsar-realm"
echo "Client ID: pulsar-client"
echo "Client Secret: pulsar-client-secret" 
echo "Test User: pulsar-user / pulsar123!"
echo "Roles: pulsar-producer, pulsar-consumer, pulsar-admin"
echo ""
echo "🔗 Access URLs:"
echo "Admin Console: $KEYCLOAK_URL/admin"
echo "Pulsar Realm: $KEYCLOAK_URL/admin/master/console/#/pulsar-realm"
echo "OIDC Discovery: $KEYCLOAK_URL/realms/pulsar-realm/.well-known/openid_configuration"
echo ""
echo "🔑 Next Steps:"
echo "1. Test token generation: ./scripts/test-pulsar-tokens.sh"
echo "2. Configure Pulsar broker with JWT settings"
echo "3. Use tokens in Pulsar client applications"