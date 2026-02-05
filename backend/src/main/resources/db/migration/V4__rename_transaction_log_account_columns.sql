-- Rename transaction log account columns (idempotent)

-- Step 1: Check if migration already applied
SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'transaction_logs'
      AND COLUMN_NAME = 'from_account_id'
);

SET @should_migrate := IF(@col_exists = 0, 1, 0);

-- Step 2: Drop old foreign key if present
SET @fk_exists := (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'transaction_logs'
      AND CONSTRAINT_NAME = 'fk_account_id'
);

SET @drop_fk := IF(@should_migrate = 1 AND @fk_exists = 1,
    'ALTER TABLE transaction_logs DROP FOREIGN KEY fk_account_id',
    'SELECT 1'
);

PREPARE fk_stmt FROM @drop_fk;
EXECUTE fk_stmt;
DEALLOCATE PREPARE fk_stmt;

-- Step 3: Rename columns (MySQL 5.7+ compatible)
SET @rename_sql1 := IF(@should_migrate = 1,
    'ALTER TABLE transaction_logs CHANGE COLUMN account_id from_account_id BIGINT NOT NULL',
    'SELECT 1'
);

PREPARE rename1 FROM @rename_sql1;
EXECUTE rename1;
DEALLOCATE PREPARE rename1;

SET @rename_sql2 := IF(@should_migrate = 1,
    'ALTER TABLE transaction_logs CHANGE COLUMN related_transaction_id to_account_id BIGINT',
    'SELECT 1'
);

PREPARE rename2 FROM @rename_sql2;
EXECUTE rename2;
DEALLOCATE PREPARE rename2;

-- Step 4: Add new foreign key if missing
SET @fk_exists_new := (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'transaction_logs'
      AND CONSTRAINT_NAME = 'fk_from_account_id'
);

SET @add_fk := IF(@fk_exists_new = 0,
    'ALTER TABLE transaction_logs ADD CONSTRAINT fk_from_account_id FOREIGN KEY (from_account_id) REFERENCES accounts(id) ON DELETE RESTRICT',
    'SELECT 1'
);

PREPARE add_fk FROM @add_fk;
EXECUTE add_fk;
DEALLOCATE PREPARE add_fk;

-- Step 5: Update indexes
SET @idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'transaction_logs'
      AND INDEX_NAME = 'idx_account_id'
);

SET @drop_idx := IF(@idx_exists = 1,
    'ALTER TABLE transaction_logs DROP INDEX idx_account_id',
    'SELECT 1'
);

PREPARE idx_stmt FROM @drop_idx;
EXECUTE idx_stmt;
DEALLOCATE PREPARE idx_stmt;

SET @new_idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'transaction_logs'
      AND INDEX_NAME = 'idx_from_account_id'
);

SET @add_idx := IF(@new_idx_exists = 0,
    'CREATE INDEX idx_from_account_id ON transaction_logs (from_account_id)',
    'SELECT 1'
);

PREPARE add_idx FROM @add_idx;
EXECUTE add_idx;
DEALLOCATE PREPARE add_idx;