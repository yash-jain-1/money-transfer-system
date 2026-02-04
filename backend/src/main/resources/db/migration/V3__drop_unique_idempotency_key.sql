-- V3__drop_unique_idempotency_key.sql
-- Allow debit/credit pairs to share the same idempotency key

ALTER TABLE transaction_logs
    DROP INDEX idx_idempotency_key;

CREATE INDEX idx_idempotency_key ON transaction_logs (idempotency_key);
