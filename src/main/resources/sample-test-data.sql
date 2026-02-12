-- Sample Test Data for Database Comparison
-- Run this script to create test tables and data in both databases

-- ========================================
-- DATABASE 1 (Source Database)
-- ========================================

-- Create test table in database1
CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100),
    status VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert test data in database1
INSERT INTO users (id, name, email, status) VALUES
(1, 'John Doe', 'john.doe@example.com', 'active'),
(2, 'Jane Smith', 'jane.smith@example.com', 'active'),
(3, 'Bob Johnson', 'bob.johnson@example.com', 'inactive'),
(4, 'Alice Williams', 'alice.williams@example.com', 'active'),
(5, 'Charlie Brown', 'charlie.brown@example.com', 'active');

-- ========================================
-- DATABASE 2 (Target Database)
-- ========================================

-- Create test table in database2
CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100),
    status VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert test data in database2 with some differences
INSERT INTO users (id, name, email, status) VALUES
(1, 'John Doe', 'john.doe.updated@example.com', 'active'),  -- Email is different
(2, 'Jane Smith', 'jane.smith@example.com', 'inactive'),     -- Status is different
(3, 'Bob Johnson', 'bob.johnson@example.com', 'inactive'),   -- Same as DB1
(4, 'Alice Williams', 'alice@example.com', 'pending'),       -- Email and status different
-- Record with id=5 is missing in DB2
(6, 'David Miller', 'david.miller@example.com', 'active');   -- New record not in DB1

-- ========================================
-- Expected Differences:
-- ========================================
-- Record ID 1: email field differs
-- Record ID 2: status field differs
-- Record ID 4: email and status fields differ
-- Record ID 5: missing in DB2
-- Record ID 6: missing in DB1
--
-- Total: 5 records with differences
-- Total field differences: 6
-- ========================================

-- ========================================
-- Configuration for application.properties
-- ========================================
-- comparison.table.name=users
-- comparison.mandatory.fields=id,name,email,status
-- comparison.primary.key.field=id
-- ========================================
