CREATE SEQUENCE IF NOT EXISTS period_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS dataset_version_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS dataset_collection_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS indicator_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS indicator_year_entry_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS region_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS observation_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS period
(
    id          BIGINT PRIMARY KEY DEFAULT NEXTVAL('period_seq'),
    period_type VARCHAR(16)  NOT NULL,
    year        INTEGER      NOT NULL,
    month       INTEGER      NOT NULL,
    quarter     INTEGER      NOT NULL,
    label       VARCHAR(100) NOT NULL,
    CONSTRAINT uk_period_period_type_year_month_quarter UNIQUE (period_type, year, month, quarter)
);

CREATE TABLE IF NOT EXISTS dataset_version
(
    id                     BIGINT PRIMARY KEY DEFAULT NEXTVAL('dataset_version_seq'),
    source_system_code     VARCHAR(64)              NOT NULL,
    external_title         VARCHAR(500)             NOT NULL,
    external_date_modified TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_dataset_version_source_system_code_external_title_external_date_modified UNIQUE (source_system_code, external_title, external_date_modified)
);

CREATE TABLE IF NOT EXISTS dataset_collection
(
    id                 BIGINT PRIMARY KEY DEFAULT NEXTVAL('dataset_collection_seq'),
    dataset_version_id BIGINT                   NOT NULL,
    collected_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    request            VARCHAR(4000)            NOT NULL,
    raw_data           JSONB                    NOT NULL,
    CONSTRAINT fk_dataset_collection_dataset_version FOREIGN KEY (dataset_version_id) REFERENCES dataset_version (id)
);

CREATE TABLE IF NOT EXISTS indicator
(
    id                   BIGINT PRIMARY KEY DEFAULT NEXTVAL('indicator_seq'),
    name                 VARCHAR(1000) NOT NULL,
    indicator_group_code VARCHAR(32)   NOT NULL,
    CONSTRAINT uk_indicator_name_indicator_group_code UNIQUE (name, indicator_group_code)
);

CREATE TABLE IF NOT EXISTS indicator_year_entry
(
    id                             BIGINT PRIMARY KEY DEFAULT NEXTVAL('indicator_year_entry_seq'),
    period_id                      BIGINT  NOT NULL,
    indicator_id                   BIGINT  NOT NULL,
    parent_indicator_year_entry_id BIGINT  NULL,
    level                          INTEGER NOT NULL,
    sort_order                     INTEGER NOT NULL,
    has_children                   BOOLEAN NOT NULL,
    CONSTRAINT uk_indicator_year_entry_indicator_id_period_id UNIQUE (indicator_id, period_id),
    CONSTRAINT fk_indicator_year_entry_period FOREIGN KEY (period_id) REFERENCES period (id),
    CONSTRAINT fk_indicator_year_entry_indicator FOREIGN KEY (indicator_id) REFERENCES indicator (id),
    CONSTRAINT fk_indicator_year_entry_parent FOREIGN KEY (parent_indicator_year_entry_id) REFERENCES indicator_year_entry (id)

);

CREATE TABLE IF NOT EXISTS region
(
    id                    BIGINT PRIMARY KEY DEFAULT NEXTVAL('region_seq'),
    code                  VARCHAR(32)  NOT NULL,
    name                  VARCHAR(255) NOT NULL,
    federal_district_code VARCHAR(16)  NOT NULL,
    CONSTRAINT uk_region_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS observation
(
    id                      BIGINT PRIMARY KEY DEFAULT NEXTVAL('observation_seq'),
    dataset_collection_id   BIGINT         NOT NULL,
    region_id               BIGINT         NOT NULL,
    indicator_year_entry_id BIGINT         NOT NULL,
    period_id               BIGINT         NOT NULL,
    observation_value_kind  VARCHAR(128)   NOT NULL,
    value                   DECIMAL(24, 8) NOT NULL,
    CONSTRAINT uk_observation_region_id_indicator_year_entry_id_period_id_observation_value_kind UNIQUE (region_id, indicator_year_entry_id, period_id, observation_value_kind),
    CONSTRAINT fk_observation_dataset_collection FOREIGN KEY (dataset_collection_id) REFERENCES dataset_collection (id),
    CONSTRAINT fk_observation_region FOREIGN KEY (region_id) REFERENCES region (id),
    CONSTRAINT fk_observation_indicator_year_entry FOREIGN KEY (indicator_year_entry_id) REFERENCES indicator_year_entry (id),
    CONSTRAINT fk_observation_period FOREIGN KEY (period_id) REFERENCES period (id)

);

CREATE INDEX IF NOT EXISTS idx_dataset_collection_dataset_version_id_collected_at
    ON dataset_collection (dataset_version_id, collected_at);

CREATE INDEX IF NOT EXISTS idx_region_code
    ON region (code);

CREATE INDEX IF NOT EXISTS idx_region_federal_district_code
    ON region (federal_district_code);

CREATE INDEX IF NOT EXISTS idx_region_name
    ON region (name);

CREATE INDEX IF NOT EXISTS idx_indicator_year_entry_parent_sort
    ON indicator_year_entry (parent_indicator_year_entry_id, sort_order);

CREATE INDEX IF NOT EXISTS idx_indicator_year_entry_period_sort
    ON indicator_year_entry (period_id, sort_order);

CREATE INDEX IF NOT EXISTS idx_observation_dataset_collection_id
    ON observation (dataset_collection_id);

CREATE INDEX IF NOT EXISTS idx_observation_indicator_year_entry_id_period_id
    ON observation (indicator_year_entry_id, period_id);

CREATE INDEX IF NOT EXISTS idx_observation_period_id
    ON observation (period_id);

CREATE INDEX IF NOT EXISTS idx_observation_region_id_period_id
    ON observation (region_id, period_id);

