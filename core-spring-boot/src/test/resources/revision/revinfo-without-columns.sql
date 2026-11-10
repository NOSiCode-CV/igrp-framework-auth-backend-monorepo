-- A service schema from before v3-24: Hibernate's default revinfo, without username/trace_id.
CREATE SEQUENCE IF NOT EXISTS revinfo_seq START WITH 1 INCREMENT BY 50;
CREATE TABLE IF NOT EXISTS revinfo (rev INTEGER NOT NULL PRIMARY KEY, revtstmp BIGINT);
