#!/bin/bash

# Keycloak and MariaDB Backup Script
# Creates backups of MariaDB database and Keycloak configuration

set -e

# Configuration
BACKUP_DIR="./backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
DB_CONTAINER="keycloak-mariadb"
KC_CONTAINER="keycloak"
DB_NAME="keycloak"
DB_USER="admin"
DB_PASSWORD="admin!34"

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

# Function to check if containers are running
check_containers() {
    log_info "Checking container status..."
    
    if ! docker ps | grep -q "$DB_CONTAINER"; then
        log_error "MariaDB container is not running"
        return 1
    fi
    
    if ! docker ps | grep -q "$KC_CONTAINER"; then
        log_warning "Keycloak container is not running (realm export will be skipped)"
        return 2
    fi
    
    log_success "Containers are running"
    return 0
}

# Function to create backup directory
create_backup_dir() {
    local backup_path="$BACKUP_DIR/$TIMESTAMP"
    
    if [ ! -d "$backup_path" ]; then
        mkdir -p "$backup_path"
        log_success "Created backup directory: $backup_path"
    fi
    
    echo "$backup_path"
}

# Function to backup MariaDB database
backup_database() {
    local backup_path=$1
    local db_backup_file="$backup_path/keycloak_db_$TIMESTAMP.sql"
    
    log_info "Backing up MariaDB database..."
    
    if docker exec "$DB_CONTAINER" mysqldump \
        -u"$DB_USER" \
        -p"$DB_PASSWORD" \
        --single-transaction \
        --routines \
        --triggers \
        --events \
        --hex-blob \
        "$DB_NAME" > "$db_backup_file"; then
        
        log_success "Database backup created: $db_backup_file"
        
        # Compress the backup
        gzip "$db_backup_file"
        log_success "Database backup compressed: ${db_backup_file}.gz"
        
        return 0
    else
        log_error "Database backup failed"
        return 1
    fi
}

# Function to export Keycloak realms
export_realms() {
    local backup_path=$1
    local realms_dir="$backup_path/realms"
    
    log_info "Exporting Keycloak realms..."
    
    mkdir -p "$realms_dir"
    
    # Get list of realms
    local realms=$(docker exec "$KC_CONTAINER" bash -c "/opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080 --realm master --user admin --password admin!34 >/dev/null 2>&1 && /opt/keycloak/bin/kcadm.sh get realms --fields realm" 2>/dev/null | grep '"realm"' | cut -d'"' -f4 || echo "master")
    
    for realm in $realms; do
        local realm_file="$realms_dir/${realm}_realm_$TIMESTAMP.json"
        
        log_info "Exporting realm: $realm"
        
        if docker exec "$KC_CONTAINER" bash -c "/opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080 --realm master --user admin --password admin!34 >/dev/null 2>&1 && /opt/keycloak/bin/kcadm.sh get realms/$realm" > "$realm_file" 2>/dev/null; then
            log_success "Realm '$realm' exported: $realm_file"
        else
            log_warning "Failed to export realm: $realm"
            rm -f "$realm_file"
        fi
    done
}

# Function to backup configuration files
backup_configs() {
    local backup_path=$1
    local config_backup_dir="$backup_path/config"
    
    log_info "Backing up configuration files..."
    
    if [ -d "./config" ]; then
        cp -r "./config" "$config_backup_dir"
        log_success "Configuration files backed up to: $config_backup_dir"
    else
        log_warning "Config directory not found"
    fi
    
    # Backup docker-compose.yml and .env files
    if [ -f "./docker-compose.yml" ]; then
        cp "./docker-compose.yml" "$backup_path/"
        log_success "docker-compose.yml backed up"
    fi
    
    if [ -f "./.env" ]; then
        cp "./.env" "$backup_path/"
        log_success ".env file backed up"
    fi
}

# Function to create backup manifest
create_manifest() {
    local backup_path=$1
    local manifest_file="$backup_path/backup_manifest.txt"
    
    log_info "Creating backup manifest..."
    
    cat > "$manifest_file" << EOF
Keycloak Backup Manifest
========================
Backup Date: $(date)
Backup Directory: $backup_path
Keycloak Version: $(docker exec "$KC_CONTAINER" cat /opt/keycloak/version.txt 2>/dev/null || echo "Unknown")
MariaDB Version: $(docker exec "$DB_CONTAINER" mysql --version 2>/dev/null || echo "Unknown")

Files Included:
$(find "$backup_path" -type f -exec basename {} \; | sort)

Backup Contents:
- Database: Full MariaDB dump with schema and data
- Realms: Exported realm configurations
- Config: Keycloak and MariaDB configuration files
- Docker: docker-compose.yml and environment files

Restore Instructions:
1. Stop running containers: docker-compose down
2. Restore database: gunzip -c keycloak_db_*.sql.gz | docker exec -i keycloak-mariadb mysql -uadmin -padmin!34 keycloak
3. Start containers: docker-compose up -d
4. Import realms manually through admin console if needed
EOF

    log_success "Backup manifest created: $manifest_file"
}

# Function to cleanup old backups
cleanup_old_backups() {
    local keep_days=${1:-7}
    
    log_info "Cleaning up backups older than $keep_days days..."
    
    if [ -d "$BACKUP_DIR" ]; then
        find "$BACKUP_DIR" -type d -name "20*" -mtime +$keep_days -exec rm -rf {} \; 2>/dev/null || true
        log_success "Old backups cleaned up"
    fi
}

# Function to display backup summary
display_summary() {
    local backup_path=$1
    local backup_size=$(du -sh "$backup_path" | cut -f1)
    
    log_success "Backup completed successfully!"
    echo
    echo "=== Backup Summary ==="
    echo "Backup Location: $backup_path"
    echo "Backup Size: $backup_size"
    echo "Backup Timestamp: $TIMESTAMP"
    echo
    echo "=== Restore Command ==="
    echo "To restore database:"
    echo "gunzip -c $backup_path/keycloak_db_$TIMESTAMP.sql.gz | \\"
    echo "docker exec -i $DB_CONTAINER mysql -u$DB_USER -p$DB_PASSWORD $DB_NAME"
    echo
}

# Main backup function
main() {
    log_info "Starting Keycloak backup process..."
    
    # Check container status
    local container_status=0
    check_containers || container_status=$?
    
    if [ $container_status -eq 1 ]; then
        log_error "Cannot proceed without MariaDB container"
        exit 1
    fi
    
    # Create backup directory
    local backup_path
    backup_path=$(create_backup_dir)
    
    # Backup database
    if ! backup_database "$backup_path"; then
        log_error "Database backup failed"
        exit 1
    fi
    
    # Export realms (only if Keycloak is running)
    if [ $container_status -ne 2 ]; then
        export_realms "$backup_path"
    fi
    
    # Backup configuration files
    backup_configs "$backup_path"
    
    # Create manifest
    create_manifest "$backup_path"
    
    # Cleanup old backups
    cleanup_old_backups 7
    
    # Display summary
    display_summary "$backup_path"
}

# Print usage information
usage() {
    echo "Usage: $0 [OPTIONS]"
    echo
    echo "Options:"
    echo "  --help        Show this help message"
    echo "  --cleanup N   Clean up backups older than N days (default: 7)"
    echo
    echo "Examples:"
    echo "  $0                Create a full backup"
    echo "  $0 --cleanup 14   Create backup and cleanup files older than 14 days"
}

# Parse command line arguments
case "${1:-}" in
    --help|-h)
        usage
        exit 0
        ;;
    --cleanup)
        if [[ $# -eq 2 && $2 =~ ^[0-9]+$ ]]; then
            cleanup_old_backups "$2"
        else
            log_error "Invalid cleanup parameter. Use: --cleanup N (where N is number of days)"
            exit 1
        fi
        exit 0
        ;;
    "")
        main
        ;;
    *)
        log_error "Unknown option: $1"
        usage
        exit 1
        ;;
esac