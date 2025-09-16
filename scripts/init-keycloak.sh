#!/bin/bash

# Keycloak Initialization Script
# This script helps initialize and configure Keycloak after startup

set -e

# Configuration
KEYCLOAK_URL="http://localhost:18200"
ADMIN_USER="admin"
ADMIN_PASSWORD="admin!34"
REALM_NAME="master"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to wait for Keycloak to be ready
wait_for_keycloak() {
    log_info "Waiting for Keycloak to start..."
    
    local max_attempts=60
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if curl -f -s "$KEYCLOAK_URL/health/ready" > /dev/null 2>&1; then
            log_success "Keycloak is ready!"
            return 0
        fi
        
        log_info "Attempt $attempt/$max_attempts: Keycloak not ready yet..."
        sleep 5
        attempt=$((attempt + 1))
    done
    
    log_error "Keycloak failed to start within expected time"
    return 1
}

# Function to get admin access token
get_admin_token() {
    log_info "Getting admin access token..."
    
    local response=$(curl -s -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "username=$ADMIN_USER" \
        -d "password=$ADMIN_PASSWORD" \
        -d "grant_type=password" \
        -d "client_id=admin-cli")
    
    if echo "$response" | grep -q "access_token"; then
        echo "$response" | grep -o '"access_token":"[^"]*' | cut -d'"' -f4
    else
        log_error "Failed to get admin token"
        echo "$response"
        return 1
    fi
}

# Function to create a test realm (optional)
create_test_realm() {
    local token=$1
    log_info "Creating test realm..."
    
    local realm_config='{
        "realm": "test-realm",
        "enabled": true,
        "displayName": "Test Realm",
        "accessTokenLifespan": 300,
        "accessTokenLifespanForImplicitFlow": 900,
        "ssoSessionIdleTimeout": 1800,
        "ssoSessionMaxLifespan": 36000,
        "offlineSessionIdleTimeout": 2592000,
        "accessCodeLifespan": 60,
        "accessCodeLifespanUserAction": 300,
        "accessCodeLifespanLogin": 1800,
        "notBefore": 0,
        "revokeRefreshToken": false,
        "refreshTokenMaxReuse": 0,
        "accessTokenLifespanUserAction": 300,
        "refreshTokenLifespan": 1800,
        "refreshTokenLifespanUserAction": 300,
        "attributes": {
            "frontendUrl": "http://localhost:18200"
        }
    }'
    
    local response=$(curl -s -w "%{http_code}" -X POST "$KEYCLOAK_URL/admin/realms" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json" \
        -d "$realm_config")
    
    local http_code="${response: -3}"
    
    if [ "$http_code" = "201" ]; then
        log_success "Test realm created successfully"
    elif [ "$http_code" = "409" ]; then
        log_warning "Test realm already exists"
    else
        log_error "Failed to create test realm (HTTP $http_code)"
    fi
}

# Function to create a test client
create_test_client() {
    local token=$1
    local realm=${2:-"test-realm"}
    
    log_info "Creating test client in realm: $realm"
    
    local client_config='{
        "clientId": "test-client",
        "name": "Test Client",
        "description": "Test client for token generation",
        "enabled": true,
        "clientAuthenticatorType": "client-secret",
        "secret": "test-client-secret",
        "redirectUris": ["http://localhost:*"],
        "webOrigins": ["http://localhost:*"],
        "standardFlowEnabled": true,
        "implicitFlowEnabled": false,
        "directAccessGrantsEnabled": true,
        "serviceAccountsEnabled": true,
        "publicClient": false,
        "frontchannelLogout": false,
        "protocol": "openid-connect",
        "attributes": {
            "saml.assertion.signature": "false",
            "saml.force.post.binding": "false",
            "saml.multivalued.roles": "false",
            "saml.encrypt": "false",
            "saml.server.signature": "false",
            "saml.server.signature.keyinfo.ext": "false",
            "exclude.session.state.from.auth.response": "false",
            "saml_force_name_id_format": "false",
            "saml.client.signature": "false",
            "tls.client.certificate.bound.access.tokens": "false",
            "saml.authnstatement": "false",
            "display.on.consent.screen": "false",
            "saml.onetimeuse.condition": "false"
        }
    }'
    
    local response=$(curl -s -w "%{http_code}" -X POST "$KEYCLOAK_URL/admin/realms/$realm/clients" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json" \
        -d "$client_config")
    
    local http_code="${response: -3}"
    
    if [ "$http_code" = "201" ]; then
        log_success "Test client created successfully"
    elif [ "$http_code" = "409" ]; then
        log_warning "Test client already exists"
    else
        log_error "Failed to create test client (HTTP $http_code)"
    fi
}

# Function to display useful information
display_info() {
    log_success "Keycloak setup completed!"
    echo
    echo "=== Keycloak Information ==="
    echo "Admin Console: $KEYCLOAK_URL/admin"
    echo "Admin User: $ADMIN_USER"
    echo "Admin Password: $ADMIN_PASSWORD"
    echo
    echo "=== Test Configuration ==="
    echo "Test Realm: test-realm"
    echo "Test Client ID: test-client"
    echo "Test Client Secret: test-client-secret"
    echo
    echo "=== Token Endpoints ==="
    echo "Master Realm Token: $KEYCLOAK_URL/realms/master/protocol/openid-connect/token"
    echo "Test Realm Token: $KEYCLOAK_URL/realms/test-realm/protocol/openid-connect/token"
    echo
    echo "=== Example Token Request ==="
    echo "curl -X POST '$KEYCLOAK_URL/realms/test-realm/protocol/openid-connect/token' \\"
    echo "  -H 'Content-Type: application/x-www-form-urlencoded' \\"
    echo "  -d 'grant_type=client_credentials' \\"
    echo "  -d 'client_id=test-client' \\"
    echo "  -d 'client_secret=test-client-secret'"
    echo
}

# Main execution
main() {
    log_info "Starting Keycloak initialization..."
    
    # Wait for Keycloak to be ready
    if ! wait_for_keycloak; then
        exit 1
    fi
    
    # Get admin token
    log_info "Authenticating as admin..."
    local admin_token
    if ! admin_token=$(get_admin_token); then
        log_error "Failed to authenticate"
        exit 1
    fi
    
    log_success "Admin authentication successful"
    
    # Optional: Create test realm and client
    if [ "${1:-}" = "--setup-test" ]; then
        create_test_realm "$admin_token"
        create_test_client "$admin_token"
    fi
    
    # Display information
    display_info
}

# Check if script is being sourced or executed
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi