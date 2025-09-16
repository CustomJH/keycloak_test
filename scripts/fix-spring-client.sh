#!/bin/bash

# Fix Spring Backend Client Configuration Script
# Recreates the backend client with proper settings

set -e

KEYCLOAK_URL="http://localhost:18200"
ADMIN_USER="admin"
ADMIN_PASSWORD="admin!34"

echo "🔧 Fixing Spring Backend Client Configuration"
echo "============================================="

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

# Check if backend client exists and delete it
echo "📋 Checking existing backend client..."
EXISTING_CLIENT=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/spring-app-realm/clients?clientId=spring-backend-client" \
    -H "Authorization: Bearer $ADMIN_TOKEN")

CLIENT_ID=$(echo $EXISTING_CLIENT | jq -r '.[0].id' 2>/dev/null)

if [ "$CLIENT_ID" != "null" ] && [ -n "$CLIENT_ID" ] && [ "$CLIENT_ID" != "" ]; then
    echo "🗑️  Deleting existing backend client..."
    curl -s -X DELETE "$KEYCLOAK_URL/admin/realms/spring-app-realm/clients/$CLIENT_ID" \
        -H "Authorization: Bearer $ADMIN_TOKEN"
    echo "✅ Existing client deleted"
fi

# Create new backend client with simpler configuration
echo "📋 Creating new spring-backend-client..."
curl -s -X POST "$KEYCLOAK_URL/admin/realms/spring-app-realm/clients" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
        "clientId": "spring-backend-client",
        "name": "Spring Boot Backend Application",
        "description": "Confidential client for Spring Boot backend",
        "enabled": true,
        "clientAuthenticatorType": "client-secret",
        "secret": "spring-backend-secret",
        "standardFlowEnabled": false,
        "implicitFlowEnabled": false,
        "directAccessGrantsEnabled": false,
        "serviceAccountsEnabled": true,
        "publicClient": false,
        "bearerOnly": false,
        "protocol": "openid-connect",
        "fullScopeAllowed": true,
        "attributes": {
            "access.token.lifespan": "3600"
        }
    }'

if [ $? -eq 0 ]; then
    echo "✅ Backend client created successfully"
else
    echo "❌ Failed to create backend client"
    exit 1
fi

# Test the new client
echo "📋 Testing new backend client..."
TEST_RESPONSE=$(curl -s -X POST "$KEYCLOAK_URL/realms/spring-app-realm/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=client_credentials" \
    -d "client_id=spring-backend-client" \
    -d "client_secret=spring-backend-secret")

ACCESS_TOKEN=$(echo $TEST_RESPONSE | jq -r '.access_token')

if [ "$ACCESS_TOKEN" != "null" ] && [ -n "$ACCESS_TOKEN" ]; then
    echo "✅ Backend client is working correctly!"
    echo "Token obtained: ${ACCESS_TOKEN:0:50}..."
else
    echo "❌ Backend client test failed"
    echo "Response: $TEST_RESPONSE"
fi

echo ""
echo "🎯 Backend Client Fixed!"
echo "======================="
echo "Client ID: spring-backend-client"
echo "Client Secret: spring-backend-secret"
echo "Service Account Enabled: Yes"
echo "Grant Type: client_credentials"