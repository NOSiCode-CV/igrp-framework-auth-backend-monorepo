-- The test service's own Flyway migration: the audit's tables must not get in its way.
CREATE TABLE IF NOT EXISTS service_invoice (id BIGINT PRIMARY KEY);
