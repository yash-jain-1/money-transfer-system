-- Ensure transaction_logs columns align with the current entity model

SET @from_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'transaction_logs'
      AND COLUMN_NAME = 'from_account_id'
);

SET @account_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'transaction_logs'
      AND COLUMN_NAME = 'account_id'
);

SET @to_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'transaction_logs'
      AND COLUMN_NAME = 'to_account_id'
);

SET @related_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'transaction_logs'
      AND COLUMN_NAME = 'related_transaction_id'
);

-- Drop any existing foreign key that depends on account_id
SET @fk_account_id := (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'transaction_logs'
      AND COLUMN_NAME = 'account_id'
      AND REFERENCED_TABLE_NAME = 'accounts'
    LIMIT 1
);

SET @drop_fk_account_id := IF(@fk_account_id IS NOT NULL,
    CONCAT('ALTER TABLE transaction_logs DROP FOREIGN KEY ', @fk_account_id),
    'SELECT 1'
);

PREPARE drop_fk_stmt FROM @drop_fk_account_id;
EXECUTE drop_fk_stmt;
DEALLOCATE PREPARE drop_fk_stmt;

-- Rename account_id to from_account_id if needed
SET @rename_from := IF(@from_exists = 0 AND @account_exists = 1,
    'ALTER TABLE transaction_logs CHANGE COLUMN account_id from_account_id BIGINT NOT NULL',
    'SELECT 1'
);

PREPARE rename_from_stmt FROM @rename_from;
EXECUTE rename_from_stmt;
DEALLOCATE PREPARE rename_from_stmt;

-- Rename related_transaction_id to to_account_id if needed
SET @rename_to := IF(@to_exists = 0 AND @related_exists = 1,
    'ALTER TABLE transaction_logs CHANGE COLUMN related_transaction_id to_account_id BIGINT',
    'SELECT 1'
);

PREPARE rename_to_stmt FROM @rename_to;
EXECUTE rename_to_stmt;
DEALLOCATE PREPARE rename_to_stmt;

-- Add new foreign key if missing
SET @fk_from_account_id := (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'transaction_logs'
      AND COLUMN_NAME = 'from_account_id'
      AND REFERENCED_TABLE_NAME = 'accounts'
    LIMIT 1
);

SET @add_fk := IF(@fk_from_account_id IS NULL,
    'ALTER TABLE transaction_logs ADD CONSTRAINT fk_from_account_id FOREIGN KEY (from_account_id) REFERENCES accounts(id) ON DELETE RESTRICT',
    'SELECT 1'
);

PREPARE add_fk_stmt FROM @add_fk;
EXECUTE add_fk_stmt;
DEALLOCATE PREPARE add_fk_stmt;

-- Update indexes
-- Keep idx_account_id to avoid FK dependency issues during transition

SET @idx_new_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'transaction_logs'
      AND INDEX_NAME = 'idx_from_account_id'
);

SET @add_idx := IF(@idx_new_exists = 0,
    'CREATE INDEX idx_from_account_id ON transaction_logs (from_account_id)',
    'SELECT 1'
);

PREPARE add_idx_stmt FROM @add_idx;
EXECUTE add_idx_stmt;
DEALLOCATE PREPARE add_idx_stmt;
