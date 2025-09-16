#!/bin/bash

# Pulsar JWT Token Generation and Testing Script
# Tests different authentication flows for Apache Pulsar integration

set -e

KEYCLOAK_URL="http://localhost:18200"
REALM="pulsar-realm"
CLIENT_ID="pulsar-client"
CLIENT_SECRET="pulsar-client-secret"
TEST_USER="pulsar-user"
TEST_PASSWORD="pulsar123!"

echo "🔐 Pulsar-Keycloak JWT Token Testing"
echo "====================================="

# Function to decode JWT (header and payload only)
decode_jwt() {
    local token=$1
    echo "📋 JWT Token Analysis:"
    echo "Header:"
    local header=$(echo $token | cut -d'.' -f1)
    # Add padding if needed for base64 decoding
    local padded_header="${header}$(printf '%0.1s' '=' '{1..4}')"
    echo $padded_header | base64 -D 2>/dev/null | jq . || echo "Could not decode header"
    echo ""
    echo "Payload:"
    local payload=$(echo $token | cut -d'.' -f2)
    local padded_payload="${payload}$(printf '%0.1s' '=' '{1..4}')"
    echo $padded_payload | base64 -D 2>/dev/null | jq . || echo "Could not decode payload"
    echo ""
    echo "Raw Token (first 50 chars): ${token:0:50}..."
    echo ""
}

# Test 1: Client Credentials Flow (Service-to-Service)
echo "🚀 Test 1: Client Credentials Flow (Service Authentication)"
echo "--------------------------------------------------------"
echo "This flow is used for Pulsar service accounts and automated systems."

CLIENT_TOKEN_RESPONSE=$(curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=client_credentials" \
    -d "client_id=$CLIENT_ID" \
    -d "client_secret=$CLIENT_SECRET")

CLIENT_ACCESS_TOKEN=$(echo $CLIENT_TOKEN_RESPONSE | jq -r '.access_token')
CLIENT_TOKEN_TYPE=$(echo $CLIENT_TOKEN_RESPONSE | jq -r '.token_type')
CLIENT_EXPIRES_IN=$(echo $CLIENT_TOKEN_RESPONSE | jq -r '.expires_in')

if [ "$CLIENT_ACCESS_TOKEN" != "null" ] && [ -n "$CLIENT_ACCESS_TOKEN" ]; then
    echo "✅ Client Credentials Token obtained successfully"
    echo "Token Type: $CLIENT_TOKEN_TYPE"
    echo "Expires In: $CLIENT_EXPIRES_IN seconds"
    echo ""
    decode_jwt "$CLIENT_ACCESS_TOKEN"
    
    # Save token for Pulsar usage
    echo "$CLIENT_ACCESS_TOKEN" > /tmp/pulsar-service-token.jwt
    echo "💾 Service token saved to /tmp/pulsar-service-token.jwt"
else
    echo "❌ Failed to get client credentials token"
    echo "Response: $CLIENT_TOKEN_RESPONSE"
fi

echo ""
echo "🚀 Test 2: Resource Owner Password Flow (User Authentication)"
echo "-----------------------------------------------------------"
echo "This flow is used for user-specific Pulsar client applications."

USER_TOKEN_RESPONSE=$(curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=password" \
    -d "client_id=$CLIENT_ID" \
    -d "client_secret=$CLIENT_SECRET" \
    -d "username=$TEST_USER" \
    -d "password=$TEST_PASSWORD")

USER_ACCESS_TOKEN=$(echo $USER_TOKEN_RESPONSE | jq -r '.access_token')
USER_REFRESH_TOKEN=$(echo $USER_TOKEN_RESPONSE | jq -r '.refresh_token')
USER_TOKEN_TYPE=$(echo $USER_TOKEN_RESPONSE | jq -r '.token_type')
USER_EXPIRES_IN=$(echo $USER_TOKEN_RESPONSE | jq -r '.expires_in')

if [ "$USER_ACCESS_TOKEN" != "null" ] && [ -n "$USER_ACCESS_TOKEN" ]; then
    echo "✅ User Password Token obtained successfully"
    echo "Token Type: $USER_TOKEN_TYPE"
    echo "Expires In: $USER_EXPIRES_IN seconds"
    echo "Has Refresh Token: $([ "$USER_REFRESH_TOKEN" != "null" ] && echo "Yes" || echo "No")"
    echo ""
    decode_jwt "$USER_ACCESS_TOKEN"
    
    # Save token for Pulsar usage
    echo "$USER_ACCESS_TOKEN" > /tmp/pulsar-user-token.jwt
    echo "💾 User token saved to /tmp/pulsar-user-token.jwt"
else
    echo "❌ Failed to get user password token"
    echo "Response: $USER_TOKEN_RESPONSE"
fi

echo ""
echo "🚀 Test 3: Token Introspection (Validation)"
echo "------------------------------------------"

if [ "$CLIENT_ACCESS_TOKEN" != "null" ] && [ -n "$CLIENT_ACCESS_TOKEN" ]; then
    INTROSPECTION_RESPONSE=$(curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token/introspect" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -u "$CLIENT_ID:$CLIENT_SECRET" \
        -d "token=$CLIENT_ACCESS_TOKEN")
    
    IS_ACTIVE=$(echo $INTROSPECTION_RESPONSE | jq -r '.active')
    
    if [ "$IS_ACTIVE" = "true" ]; then
        echo "✅ Service token is valid and active"
        echo "Token Subject: $(echo $INTROSPECTION_RESPONSE | jq -r '.sub')"
        echo "Token Audience: $(echo $INTROSPECTION_RESPONSE | jq -r '.aud')"
        echo "Token Issuer: $(echo $INTROSPECTION_RESPONSE | jq -r '.iss')"
        echo "Token Client: $(echo $INTROSPECTION_RESPONSE | jq -r '.client_id')"
    else
        echo "❌ Service token validation failed"
    fi
fi

echo ""
echo "🚀 Test 4: OIDC Discovery Endpoint"
echo "---------------------------------"
DISCOVERY_RESPONSE=$(curl -s "$KEYCLOAK_URL/realms/$REALM/.well-known/openid_configuration")
ISSUER=$(echo $DISCOVERY_RESPONSE | jq -r '.issuer')
JWKS_URI=$(echo $DISCOVERY_RESPONSE | jq -r '.jwks_uri')
TOKEN_ENDPOINT=$(echo $DISCOVERY_RESPONSE | jq -r '.token_endpoint')

echo "✅ OIDC Discovery Information:"
echo "Issuer: $ISSUER"
echo "JWKS URI: $JWKS_URI"
echo "Token Endpoint: $TOKEN_ENDPOINT"

echo ""
echo "🚀 Test 5: Public Key Retrieval (for JWT verification)"
echo "----------------------------------------------------"
JWKS_RESPONSE=$(curl -s "$JWKS_URI")
KEY_COUNT=$(echo $JWKS_RESPONSE | jq '.keys | length')
echo "✅ Retrieved $KEY_COUNT public keys for JWT verification"
echo "First Key ID: $(echo $JWKS_RESPONSE | jq -r '.keys[0].kid')"
echo "Algorithm: $(echo $JWKS_RESPONSE | jq -r '.keys[0].alg')"

echo ""
echo "🎯 Pulsar Configuration Summary"
echo "==============================="
echo ""
echo "📋 For Pulsar Broker Configuration (broker.conf):"
echo "authenticationEnabled=true"
echo "authorizationEnabled=true"
echo "authenticationProviders=org.apache.pulsar.broker.authentication.AuthenticationProviderToken"
echo "tokenPublicKey=file:///path/to/keycloak-public.key"
echo "# OR use JWKS URI:"
echo "tokenPublicKeyUri=$JWKS_URI"
echo "superUserRoles=pulsar-admin"
echo ""
echo "📋 For Pulsar Client Applications:"
echo "Authentication Plugin: org.apache.pulsar.client.impl.auth.AuthenticationToken"
echo "Service Token File: /tmp/pulsar-service-token.jwt"
echo "User Token File: /tmp/pulsar-user-token.jwt"
echo ""
echo "📋 Example Pulsar Client Usage:"
echo "Java:"
echo "  PulsarClient client = PulsarClient.builder()"
echo "    .serviceUrl(\"pulsar://localhost:6650\")"
echo "    .authentication(AuthenticationFactory.token(\"$(cat /tmp/pulsar-service-token.jwt 2>/dev/null || echo 'TOKEN_HERE')\"))"
echo "    .build();"
echo ""
echo "Python:"
echo "  client = pulsar.Client('pulsar://localhost:6650',"
echo "    authentication=pulsar.AuthenticationToken('$(cat /tmp/pulsar-service-token.jwt 2>/dev/null || echo 'TOKEN_HERE')'))"
echo ""
echo "CLI:"
echo "  pulsar-admin --auth-plugin org.apache.pulsar.client.impl.auth.AuthenticationToken \\"
echo "    --auth-params token:$(cat /tmp/pulsar-service-token.jwt 2>/dev/null || echo 'TOKEN_HERE') \\"
echo "    topics list public/default"
echo ""
echo "🔄 Token Refresh:"
echo "- Service tokens: Re-authenticate with client credentials"
echo "- User tokens: Use refresh token or re-authenticate"
echo ""
echo "✅ Integration testing completed!"