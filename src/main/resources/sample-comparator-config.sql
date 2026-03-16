-- Sample data for COMPARATOR_CONFIG table
-- Run this in TIBCO_MS database

-- Insert sample configuration for notification_log comparison
-- ms_service_name_whitelist: comma-separated UPDATED_BY values to filter rows in MS DB.
-- soi_service_name_whitelist: comma-separated UPDATED_BY values to filter rows in SOI DB.
-- where_date_field: the date column used in WHERE clause (e.g. LAST_UPDT_TS, CREATED_AT).
--                   Set to NULL to default to LAST_UPDT_TS.
-- Set whitelist to NULL to compare all rows regardless of UPDATED_BY.
INSERT INTO COMPARATOR_CONFIG (
    service_id,
    table_name,
    table_fields,
    primary_fields,
    execution_status,
    start_date,
    end_date,
    ms_service_name_whitelist,
    soi_service_name_whitelist,
    where_date_field
) VALUES (
    'NOTIFICATION_SERVICE',
    'notification_log',
    'notification_id,message,status,recipient,created_at',
    'notification_id',
    'N',  -- N = enabled for comparison
    '2024-01-01 00:00:00',
    '2024-12-31 23:59:59',
    'NOTIFICATION_SERVICE,ALERT_SERVICE',  -- MS DB: filter UPDATED_BY IN ('NOTIFICATION_SERVICE','ALERT_SERVICE')
    'NOTIFICATION_SOI,ALERT_SOI',          -- SOI DB: filter UPDATED_BY IN ('NOTIFICATION_SOI','ALERT_SOI')
    'LAST_UPDT_TS'                         -- date field for WHERE condition
);

-- You can add more configurations for different services/tables
-- Example for another service using a different date column:
/*
INSERT INTO COMPARATOR_CONFIG (
    service_id,
    table_name,
    table_fields,
    primary_fields,
    execution_status,
    start_date,
    end_date,
    where_date_field
) VALUES (
    'ORDER_SERVICE',
    'order_log',
    'order_id,customer_id,order_status,total_amount,created_at',
    'order_id',
    'N',
    '2024-01-01 00:00:00',
    '2024-12-31 23:59:59',
    'CREATED_AT'  -- uses CREATED_AT instead of LAST_UPDT_TS
);
*/

-- Query to view all active configurations
SELECT * FROM COMPARATOR_CONFIG WHERE execution_status = 'N';

-- Query to disable a configuration (set to 'Y' to disable)
-- UPDATE COMPARATOR_CONFIG SET execution_status = 'Y' WHERE id = 1;

-- Query to enable a configuration
-- UPDATE COMPARATOR_CONFIG SET execution_status = 'N' WHERE id = 1;
