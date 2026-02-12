  -- Schema for comparison results table
-- This table will be created in database 2 to store comparison results

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
