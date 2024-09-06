---
-- ogc audit table
---

CREATE TABLE IF NOT EXISTS auditing_ogc
(
    id VARCHAR NOT NULL,
    userid UUID NOT NULL,
    api VARCHAR NOT NULL,
    request_json JSON NOT NULL,
    resourceid UUID NOT NULL,
    providerid UUID NOT NULL,
    resource_group UUID NOT NULL,
    epochtime NUMERIC NOT NULL,
    time TIMESTAMP WITHOUT TIME ZONE,
    isotime VARCHAR NOT NULL,
    size NUMERIC NOT NULL,
    delegator_id UUID NOT NULL,
    CONSTRAINT ogc_primary_key PRIMARY KEY (id)
);

CREATE INDEX ogc_userid_index ON auditing_ogc USING HASH (userid);
CREATE INDEX ogc_providerid_index ON auditing_ogc USING HASH (providerid);
CREATE INDEX ogc_resourceid_index on auditing_ogc (resourceid,epochtime);
CREATE INDEX ogc_time_index ON auditing_ogc (time);


ALTER TABLE auditing_ogc OWNER TO ${flyway:user};

GRANT USAGE ON SCHEMA ${flyway:defaultSchema} TO ${rsUser};
GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE auditing_ogc TO ${rsUser};