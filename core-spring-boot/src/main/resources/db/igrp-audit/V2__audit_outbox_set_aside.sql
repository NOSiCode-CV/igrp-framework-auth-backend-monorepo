-- v3-22 (#32): a row Kafka can never take (larger than the broker accepts, say)
-- is set aside, so it does not hold back the rows after it. The relay records
-- when and why; the event itself stays in audit_event. To send it again once the
-- cause is fixed, clear failed_at.

ALTER TABLE audit_outbox ADD COLUMN IF NOT EXISTS failed_at TIMESTAMPTZ;
ALTER TABLE audit_outbox ADD COLUMN IF NOT EXISTS failure   VARCHAR(1000);

-- The rows still to send: neither published nor set aside.
CREATE INDEX IF NOT EXISTS audit_outbox_pending ON audit_outbox (created_at)
    WHERE published_at IS NULL AND failed_at IS NULL;
DROP INDEX IF EXISTS audit_outbox_unpublished;

GRANT UPDATE (failed_at, failure) ON audit_outbox TO "${appRole}";
