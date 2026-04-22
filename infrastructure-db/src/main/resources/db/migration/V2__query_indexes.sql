-- Non-unique indexes are kept separately from the structural schema.

CREATE INDEX IF NOT EXISTS idx_period_year
    ON period (year);

CREATE INDEX IF NOT EXISTS idx_dataset_collection_version_collected_at
    ON dataset_collection (dataset_version_id, collected_at DESC);

CREATE INDEX IF NOT EXISTS idx_region_name
    ON region (name);

CREATE INDEX IF NOT EXISTS idx_region_federal_district_code
    ON region (federal_district_code);

CREATE INDEX IF NOT EXISTS idx_indicator_year_entry_parent_sort
    ON indicator_year_entry (parent_indicator_year_entry_id, sort_order);

CREATE INDEX IF NOT EXISTS idx_indicator_year_entry_period_sort
    ON indicator_year_entry (period_id, sort_order);

CREATE INDEX IF NOT EXISTS idx_indicator_year_entry_period_level_sort
    ON indicator_year_entry (period_id, level, sort_order);

CREATE INDEX IF NOT EXISTS idx_indicator_year_entry_period_indicator
    ON indicator_year_entry (period_id, indicator_id);

CREATE INDEX IF NOT EXISTS idx_observation_dataset_collection
    ON observation (dataset_collection_id);

CREATE INDEX IF NOT EXISTS idx_observation_read_period_region_indicator_kind
    ON observation (period_id, region_id, indicator_year_entry_id, observation_value_kind);

CREATE INDEX IF NOT EXISTS idx_observation_read_period_indicator_region_kind
    ON observation (period_id, indicator_year_entry_id, region_id, observation_value_kind);
