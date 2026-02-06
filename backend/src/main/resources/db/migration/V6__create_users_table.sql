-- V6__create_users_table.sql
-- Creates users table to store user authentication and profile information
-- Establishes relationship between users and accounts

-- Drop foreign key constraint if exists from failed previous migration attempts
SET @drop_fk = (SELECT IF(
    EXISTS(
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS 
        WHERE CONSTRAINT_SCHEMA = DATABASE()
        AND TABLE_NAME = 'accounts'
        AND CONSTRAINT_NAME = 'fk_accounts_user'
        AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    ),
    'ALTER TABLE accounts DROP FOREIGN KEY fk_accounts_user',
    'SELECT 1'
));
PREPARE stmt FROM @drop_fk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop user_id column if exists
SET @drop_col = (SELECT IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'accounts'
        AND COLUMN_NAME = 'user_id'
    ),
    'ALTER TABLE accounts DROP COLUMN user_id',
    'SELECT 1'
));
PREPARE stmt FROM @drop_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop table if exists from failed previous migration attempts
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT 'Unique username for authentication',
    password VARCHAR(255) NOT NULL COMMENT 'BCrypt encoded password',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT 'User email address',
    full_name VARCHAR(100) NOT NULL COMMENT 'User full name',
    role VARCHAR(20) NOT NULL COMMENT 'User role (USER or ADMIN)',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Account enabled status',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'User creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    last_login TIMESTAMP NULL COMMENT 'Last successful login timestamp',
    reset_token VARCHAR(100) NULL COMMENT 'Password reset token',
    reset_token_expiry TIMESTAMP NULL COMMENT 'Reset token expiration timestamp',
    
    CONSTRAINT chk_valid_role CHECK (role IN ('USER', 'ADMIN')),
    
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_reset_token (reset_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Users table for authentication and authorization';

-- Add user_id column to accounts table to establish ownership
ALTER TABLE accounts 
ADD COLUMN user_id BIGINT NULL COMMENT 'Owner of this account';

ALTER TABLE accounts
ADD CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE accounts
ADD INDEX idx_user_id (user_id);

-- Insert default users (test user and admin)
-- Password for both is 'password' encoded with BCrypt
INSERT INTO users (username, password, email, full_name, role, enabled)
VALUES 
    ('testuser', '$2a$10$EzRjHPBmK3lIHzHdQOBK7eZBU2/5PpqVGDKiJv/6qYPMFPbZj8iYy', 'testuser@example.com', 'Test User', 'USER', TRUE),
    ('admin', '$2a$10$EzRjHPBmK3lIHzHdQOBK7eZBU2/5PpqVGDKiJv/6qYPMFPbZj8iYy', 'admin@example.com', 'Admin User', 'ADMIN', TRUE);

-- Link existing accounts to testuser (id=1) for backward compatibility
-- In production, you would need a proper data migration strategy
UPDATE accounts SET user_id = 1 WHERE id IN (1, 2);
