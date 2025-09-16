-- MariaDB Database Initialization Script
-- This script creates additional databases and users for the Keycloak setup

-- Create user management database
CREATE DATABASE IF NOT EXISTS `user_management` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create application database for custom user data
CREATE DATABASE IF NOT EXISTS `app_users` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Ensure admin user can connect from any host and grant permissions
CREATE USER IF NOT EXISTS 'admin'@'%' IDENTIFIED BY 'admin!34';
GRANT ALL PRIVILEGES ON `keycloak`.* TO 'admin'@'%';
GRANT ALL PRIVILEGES ON `user_management`.* TO 'admin'@'%';
GRANT ALL PRIVILEGES ON `app_users`.* TO 'admin'@'%';

-- Create sample user management tables
USE `user_management`;

CREATE TABLE IF NOT EXISTS `user_profiles` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `keycloak_user_id` VARCHAR(255) UNIQUE NOT NULL,
    `username` VARCHAR(100) NOT NULL,
    `email` VARCHAR(255) NOT NULL,
    `first_name` VARCHAR(100),
    `last_name` VARCHAR(100),
    `phone` VARCHAR(20),
    `department` VARCHAR(100),
    `role` VARCHAR(50) DEFAULT 'user',
    `status` ENUM('active', 'inactive', 'suspended') DEFAULT 'active',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_keycloak_user_id` (`keycloak_user_id`),
    INDEX `idx_username` (`username`),
    INDEX `idx_email` (`email`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_permissions` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT NOT NULL,
    `resource` VARCHAR(100) NOT NULL,
    `action` VARCHAR(50) NOT NULL,
    `granted` BOOLEAN DEFAULT TRUE,
    `granted_by` INT,
    `granted_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `expires_at` TIMESTAMP NULL,
    FOREIGN KEY (`user_id`) REFERENCES `user_profiles`(`id`) ON DELETE CASCADE,
    INDEX `idx_user_resource` (`user_id`, `resource`),
    INDEX `idx_resource_action` (`resource`, `action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_sessions` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT NOT NULL,
    `session_token` VARCHAR(255) UNIQUE NOT NULL,
    `ip_address` VARCHAR(45),
    `user_agent` TEXT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `last_activity` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `expires_at` TIMESTAMP NOT NULL,
    `is_active` BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (`user_id`) REFERENCES `user_profiles`(`id`) ON DELETE CASCADE,
    INDEX `idx_session_token` (`session_token`),
    INDEX `idx_user_active` (`user_id`, `is_active`),
    INDEX `idx_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create application-specific user data tables
USE `app_users`;

CREATE TABLE IF NOT EXISTS `user_preferences` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `keycloak_user_id` VARCHAR(255) UNIQUE NOT NULL,
    `language` VARCHAR(5) DEFAULT 'ko',
    `timezone` VARCHAR(50) DEFAULT 'Asia/Seoul',
    `theme` VARCHAR(20) DEFAULT 'light',
    `notifications_enabled` BOOLEAN DEFAULT TRUE,
    `email_notifications` BOOLEAN DEFAULT TRUE,
    `sms_notifications` BOOLEAN DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_keycloak_user_id` (`keycloak_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_activity_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `keycloak_user_id` VARCHAR(255) NOT NULL,
    `action` VARCHAR(100) NOT NULL,
    `resource` VARCHAR(255),
    `details` JSON,
    `ip_address` VARCHAR(45),
    `user_agent` TEXT,
    `timestamp` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_timestamp` (`keycloak_user_id`, `timestamp`),
    INDEX `idx_action` (`action`),
    INDEX `idx_resource` (`resource`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert sample data for testing
USE `user_management`;

INSERT IGNORE INTO `user_profiles` (`keycloak_user_id`, `username`, `email`, `first_name`, `last_name`, `department`, `role`) VALUES
('sample-admin-id', 'admin', 'admin@example.com', 'Admin', 'User', 'IT', 'admin'),
('sample-user-id', 'testuser', 'test@example.com', 'Test', 'User', 'Development', 'user');

INSERT IGNORE INTO `user_permissions` (`user_id`, `resource`, `action`) VALUES
(1, 'admin_panel', 'access'),
(1, 'user_management', 'create'),
(1, 'user_management', 'read'),
(1, 'user_management', 'update'),
(1, 'user_management', 'delete'),
(2, 'dashboard', 'access'),
(2, 'profile', 'read'),
(2, 'profile', 'update');

USE `app_users`;

INSERT IGNORE INTO `user_preferences` (`keycloak_user_id`, `language`, `timezone`) VALUES
('sample-admin-id', 'ko', 'Asia/Seoul'),
('sample-user-id', 'ko', 'Asia/Seoul');

-- Refresh privileges
FLUSH PRIVILEGES;