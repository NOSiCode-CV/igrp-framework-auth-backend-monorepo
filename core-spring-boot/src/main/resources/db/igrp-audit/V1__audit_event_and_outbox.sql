-- v3-21 (#29): the business audit of a service, written by the iGRP starter.
-- Applied by the starter's own Flyway history (igrp_audit_schema_history), so it
-- never collides with the service's migrations. Idempotent: safe on a database
-- that already has these tables.

CREATE TABLE IF NOT EXISTS audit_event (
    id          UUID         PRIMARY KEY,
    event       VARCHAR(128) NOT NULL,
    resource    VARCHAR(512) NOT NULL,
    payload     JSONB,
    username    VARCHAR(255) NOT NULL,
    service     VARCHAR(128) NOT NULL,
    agent       VARCHAR(255),
    result      VARCHAR(32)  NOT NULL,
    trace_id    VARCHAR(64)  NOT NULL,
    occurred_at TIMESTAMPTZ  NOT NULL
);

CREATE INDEX IF NOT EXISTS audit_event_resource ON audit_event (resource);

-- One row per event, holding the event as it is published to Kafka (igrp.audit).
-- The relay sets published_at after the broker acknowledges it.
-- No foreign key to audit_event: checking one locks the event row (FOR KEY SHARE),
-- which needs UPDATE on audit_event, the very privilege revoked below. The event and
-- its outbox row are written in one transaction, which keeps them together.
CREATE TABLE IF NOT EXISTS audit_outbox (
    id           UUID        PRIMARY KEY,
    event_id     UUID        NOT NULL,
    payload      JSONB       NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS audit_outbox_unpublished ON audit_outbox (created_at) WHERE published_at IS NULL;

-- Append-only by database permissions: the application's role may add events and
-- outbox rows, and may only set published_at on the outbox.
REVOKE UPDATE, DELETE, TRUNCATE ON audit_event FROM PUBLIC, "${appRole}";
GRANT SELECT, INSERT ON audit_event TO "${appRole}";

REVOKE UPDATE, DELETE, TRUNCATE ON audit_outbox FROM PUBLIC, "${appRole}";
GRANT SELECT, INSERT ON audit_outbox TO "${appRole}";
GRANT UPDATE (published_at) ON audit_outbox TO "${appRole}";
