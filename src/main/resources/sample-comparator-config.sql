-- Sample data for COMPARATOR_CONFIG table
-- Run this in TIBCO_SOI database

-- Insert sample configuration for notification_log comparison
INSERT INTO COMPARATOR_CONFIG (
    service_id,
    table_name,
    table_fields,
    primary_fields,
    comparator_execution_status,
    start_date,
    end_date
) VALUES (
    'NOTIFICATION_SERVICE',
    'notification_log',
    'notification_id,message,status,recipient,created_at',
    'notification_id',
    'N',  -- N = enabled for comparison
    '2024-01-01 00:00:00',
    '2024-12-31 23:59:59'
);

-- You can add more configurations for different services/tables
-- Example for another service:
/*
INSERT INTO COMPARATOR_CONFIG (
    service_id,
    table_name,
    table_fields,
    primary_fields,
    comparator_execution_status,
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
SELECT * FROM COMPARATOR_CONFIG WHERE comparator_execution_status = 'N';

-- Query to disable a configuration (set to 'Y' to disable)
-- UPDATE COMPARATOR_CONFIG SET comparator_execution_status = 'Y' WHERE id = 1;

-- Query to enable a configuration
-- UPDATE COMPARATOR_CONFIG SET comparator_execution_status = 'N' WHERE id = 1;
