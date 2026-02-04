-- V2__create_transaction_logs.sql
-- Transaction audit trail with idempotency guarantees

CREATE TABLE IF NOT EXISTS transaction_logs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT 'Reference to the account',
    idempotency_key VARCHAR(36) NOT NULL COMMENT 'UUID for exactly-once semantics - prevents duplicate processing',
    transaction_type VARCHAR(255) NOT NULL COMMENT 'Type of transaction (DEBIT, CREDIT, TRANSFER)',
    amount DECIMAL(19, 2) NOT NULL COMMENT 'Transaction amount',
    balance_before DECIMAL(19, 2) NOT NULL COMMENT 'Account balance before transaction',
    balance_after DECIMAL(19, 2) NOT NULL COMMENT 'Account balance after transaction',
    status VARCHAR(255) NOT NULL COMMENT 'Transaction status (PENDING, COMPLETED, FAILED, REVERSED)',
    description VARCHAR(500) COMMENT 'Transaction description or reason',
    related_transaction_id BIGINT COMMENT 'ID of related transaction (e.g., reverse transaction)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP() COMMENT 'Transaction timestamp',
    
    CONSTRAINT fk_account_id FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE RESTRICT,
    CONSTRAINT chk_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_valid_tx_type CHECK (transaction_type IN ('DEBIT', 'CREDIT', 'TRANSFER')),
    CONSTRAINT chk_valid_tx_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'REVERSED')),
    
    UNIQUE KEY idx_idempotency_key (idempotency_key),
    INDEX idx_account_id (account_id),
    INDEX idx_created_at (created_at),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Immutable transaction audit trail';
