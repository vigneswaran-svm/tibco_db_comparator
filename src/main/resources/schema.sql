-- Schema for comparison results table
-- This table will be created in database 2 to store comparison results

-- =====================================================================
-- ALTER: Add ms_service_name_whitelist, soi_service_name_whitelist,
--        and where_date_field to COMPARATOR_CONFIG
-- ms_service_name_whitelist: comma-separated UPDATED_BY values for MS DB
-- soi_service_name_whitelist: comma-separated UPDATED_BY values for SOI DB
-- where_date_field: dynamic date column name used in WHERE clause
--                   (defaults to LAST_UPDT_TS when NULL)
-- =====================================================================
ALTER TABLE COMPARATOR_CONFIG
    ADD COLUMN IF NOT EXISTS ms_service_name_whitelist MEDIUMTEXT NULL,
    ADD COLUMN IF NOT EXISTS soi_service_name_whitelist MEDIUMTEXT NULL,
    ADD COLUMN IF NOT EXISTS where_date_field VARCHAR(50) NULL;

-- If migrating from old service_name_whitelist column:
-- UPDATE COMPARATOR_CONFIG SET ms_service_name_whitelist = service_name_whitelist WHERE service_name_whitelist IS NOT NULL;
-- ALTER TABLE COMPARATOR_CONFIG DROP COLUMN IF EXISTS service_name_whitelist;

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
