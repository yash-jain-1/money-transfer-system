-- V1__create_accounts.sql
-- Initial schema creation: accounts table with optimistic locking support

CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(255) NOT NULL UNIQUE COMMENT 'Unique account identifier',
    account_holder VARCHAR(255) NOT NULL COMMENT 'Name of the account holder',
    balance DECIMAL(19, 2) NOT NULL COMMENT 'Current account balance',
    account_type VARCHAR(255) NOT NULL COMMENT 'Type of account (e.g., CHECKING, SAVINGS)',
    status VARCHAR(255) NOT NULL COMMENT 'Account status (ACTIVE, INACTIVE, SUSPENDED)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    version BIGINT DEFAULT 0 COMMENT 'Version field for optimistic locking - prevents lost updates during concurrent transactions',
    
    CONSTRAINT chk_balance_non_negative CHECK (balance >= 0),
    CONSTRAINT chk_valid_account_type CHECK (account_type IN ('CHECKING', 'SAVINGS', 'BUSINESS')),
    CONSTRAINT chk_valid_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    
    INDEX idx_account_holder (account_holder),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bank accounts table';
