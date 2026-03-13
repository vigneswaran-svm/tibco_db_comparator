-- Sample data for COMPARATOR_CONFIG table
-- Run this in TIBCO_SOI database

-- Insert sample configuration for notification_log comparison
-- service_name_whitelist: comma-separated UPDATED_BY values to filter rows in target table.
-- Set to NULL to compare all rows regardless of UPDATED_BY.
INSERT INTO COMPARATOR_CONFIG (
    service_id,
    table_name,
    table_fields,
    primary_fields,
    execution_status,
    start_date,
    end_date,
    service_name_whitelist
) VALUES (
    'NOTIFICATION_SERVICE',
    'notification_log',
    'notification_id,message,status,recipient,created_at',
    'notification_id',
    'N',  -- N = enabled for comparison
    '2024-01-01 00:00:00',
    '2024-12-31 23:59:59',
    'NOTIFICATION_SERVICE,ALERT_SERVICE'  -- only compare rows where UPDATED_BY IN ('NOTIFICATION_SERVICE','ALERT_SERVICE')
);

-- You can add more configurations for different services/tables
-- Example for another service:
/*
INSERT INTO COMPARATOR_CONFIG (
    service_id,
    table_name,
    table_fields,
    primary_fields,
    execution_status,
    start_date,
    end_date
) VALUES (
    'ORDER_SERVICE',
    'order_log',
    'order_id,customer_id,order_status,total_amount,created_at',
    'order_id',
    'N',
    '2024-01-01 00:00:00',
    '2024-12-31 23:59:59'
);
*/

-- Query to view all active configurations
SELECT * FROM COMPARATOR_CONFIG WHERE execution_status = 'N';

-- Query to disable a configuration (set to 'Y' to disable)
-- UPDATE COMPARATOR_CONFIG SET execution_status = 'Y' WHERE id = 1;

-- Query to enable a configuration
-- UPDATE COMPARATOR_CONFIG SET execution_status = 'N' WHERE id = 1;
