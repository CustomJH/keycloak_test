#!/bin/bash

# Database Connection Test Script
# Tests both root and admin access to all databases

echo "🔍 Database Connection Testing"
echo "=================================="

# Test root access from inside container
echo "📊 Testing root access (inside container):"
docker exec keycloak-mariadb mariadb -u root -p'root' -e "
    SELECT 'Root access successful' AS status;
    SHOW DATABASES;
    SELECT COUNT(*) AS total_databases FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME NOT IN ('information_schema', 'performance_schema', 'mysql', 'sys');
"

echo ""
echo "📊 Testing admin access (inside container):"
docker exec keycloak-mariadb mariadb -u admin -p'admin!34' -e "
    SELECT 'Admin access successful' AS status;
    SHOW DATABASES;
" 2>/dev/null || echo "⚠️  Admin access from inside container failed"

echo ""
echo "📊 Testing root access (external - host machine):"
mariadb -h 127.0.0.1 -P 18300 -u root -p'root' -e "
    SELECT 'External root access successful' AS status;
    SHOW DATABASES;
" 2>/dev/null || echo "⚠️  External root access failed"

echo ""
echo "📊 User Management Database Test:"
docker exec keycloak-mariadb mariadb -u root -p'root' -D user_management -e "
    SELECT 'User management DB accessible' AS status;
    SHOW TABLES;
    SELECT COUNT(*) AS sample_users FROM user_profiles;
"

echo ""
echo "📊 App Users Database Test:"
docker exec keycloak-mariadb mariadb -u root -p'root' -D app_users -e "
    SELECT 'App users DB accessible' AS status;
    SHOW TABLES;
    SELECT COUNT(*) AS sample_preferences FROM user_preferences;
"

echo ""
echo "📊 Database Users Summary:"
docker exec keycloak-mariadb mariadb -u root -p'root' -e "
    SELECT 
        User,
        Host,
        plugin,
        CASE 
            WHEN authentication_string != '' THEN 'Password Set'
            ELSE 'No Password'
        END AS password_status
    FROM mysql.user 
    WHERE User NOT IN ('mariadb.sys', 'healthcheck')
    ORDER BY User, Host;
"

echo ""
echo "🎯 Summary:"
echo "- Root account: root / root"
echo "- Admin account: admin / admin!34"
echo "- Databases: keycloak, user_management, app_users"
echo "- Port: 18300 (MariaDB), 18200 (Keycloak)"
echo ""
echo "✅ Testing completed!"