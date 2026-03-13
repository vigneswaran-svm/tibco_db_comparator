-- Schema for comparison results table
-- This table will be created in database 2 to store comparison results

-- =====================================================================
-- ALTER: Add service_name_whitelist to COMPARATOR_CONFIG
-- Comma-separated list of service names used to filter target table rows
-- by the UPDATED_BY column. Leave NULL to skip UPDATED_BY filtering.
-- =====================================================================
ALTER TABLE COMPARATOR_CONFIG
    ADD COLUMN IF NOT EXISTS service_name_whitelist TEXT NULL;

-- =====================================================================
-- ALTER: Add UPDATED_BY to CORRESPONDENCE_REQ (target table in both DBs)
-- Values correspond to the service_id in COMPARATOR_CONFIG.
-- This column is used as an additional WHERE filter during comparison.
-- =====================================================================
ALTER TABLE CORRESPONDENCE_REQ
    ADD COLUMN IF NOT EXISTS UPDATED_BY VARCHAR(100) NULL;

CREATE INDEX IF NOT EXISTS idx_correspondence_req_updated_by ON CORRESPONDENCE_REQ (UPDATED_BY);

CREATE TABLE IF NOT EXISTS comparison_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    record_id VARCHAR(255) NOT NULL,
    table_name VARCHAR(255) NOT NULL,
    field_name VARCHAR(255) NOT NULL,
    db1_value TEXT,
    db2_value TEXT,
    comparison_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_table_name (table_name),
    INDEX idx_record_id (record_id),
    INDEX idx_table_record (table_name, record_id),
    INDEX idx_comparison_date (comparison_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
