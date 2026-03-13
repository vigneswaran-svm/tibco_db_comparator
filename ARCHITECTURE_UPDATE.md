# Architecture Update - Config and History in TIBCO_MS

## Changes Made

The `COMPARATOR_CONFIG` and `COMPARATOR_HISTORY` tables are now located in **TIBCO_MS** database instead of TIBCO_SOI.

### Updated Architecture

**TIBCO_MS (db1):**
- `COMPARATOR_CONFIG` - Stores comparison configurations ✅
- `COMPARATOR_HISTORY` - Stores comparison results ✅
- `notification_log` - Source table for comparison

**TIBCO_SOI (db2):**
- `notification_log` - Target table for comparison

### Files Updated

1. **Moved Entities to db1 package:**
   - `ComparatorConfig.java` → `org.example.model.db1`
   - `ComparatorHistory.java` → `org.example.model.db1`

2. **Updated Repositories:**
   - `ComparatorConfigRepository.java` - Now uses db1 entities
   - `ComparatorHistoryRepository.java` - Now uses db1 entities

3. **Updated Configuration:**
   - `Db1RepositoryConfig.java` - Created to manage db1 repositories
   - Removed `Db2RepositoryConfig.java` - No longer needed

4. **Updated Service:**
   - `ComparisonService.java` - Uses `db1TransactionManager`
   - Imports from `org.example.model.db1`

5. **Updated Controller:**
   - `ComparisonController.java` - Imports from `org.example.model.db1`

## Database Setup

### Create Tables in TIBCO_MS

Run these SQL scripts in **TIBCO_MS** database:

```sql
-- Create COMPARATOR_CONFIG table in TIBCO_MS
CREATE TABLE IF NOT EXISTS COMPARATOR_CONFIG (
  id BIGINT NOT NULL AUTO_INCREMENT,
  service_id VARCHAR(100) NOT NULL,
  table_name VARCHAR(128) NOT NULL,
  table_fields TEXT NOT NULL COMMENT 'Comma-separated list of table fields',
  primary_fields TEXT NOT NULL COMMENT 'Comma-separated list of primary key fields',
  comparator_execution_status CHAR(1) NOT NULL DEFAULT 'N',
  start_date DATETIME NOT NULL,
  end_date DATETIME NOT NULL,
  created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP(),
  updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP(),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create COMPARATOR_HISTORY table in TIBCO_MS
CREATE TABLE IF NOT EXISTS COMPARATOR_HISTORY (
  id BIGINT NOT NULL AUTO_INCREMENT,
  service_id VARCHAR(100) NOT NULL,
  table_name VARCHAR(128) NOT NULL,
  fields_name TEXT NOT NULL,
  comparator_execution_status VARCHAR(10) NOT NULL,
  soi_table_values TEXT NOT NULL,
  ms_table_values TEXT NOT NULL,
  created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP(),
  updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP(),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### Insert Sample Configuration

```sql
-- Insert into TIBCO_MS database
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
    'N',
    '2024-01-01 00:00:00',
    '2024-12-31 23:59:59'
);
```

## How It Works Now

1. **Configuration:** Reads from `COMPARATOR_CONFIG` in TIBCO_MS
2. **Data Fetching:**
   - Fetches records from `notification_log` in TIBCO_MS (db1)
   - Fetches records from `notification_log` in TIBCO_SOI (db2)
3. **Comparison:** Compares the records based on configured fields
4. **Results Storage:** Saves differences to `COMPARATOR_HISTORY` in TIBCO_MS

## Application Configuration

Your `application.properties` is already configured correctly:

```properties
# Database 1 (TIBCO_MS - Source + Config + History)
db1.datasource.url=jdbc:mariadb://localhost:3306/TIBCO_MS
db1.datasource.username=tibco_user
db1.datasource.password=dbuser@123

# Database 2 (TIBCO_SOI - Target)
db2.datasource.url=jdbc:mariadb://localhost:3306/TIBCO_SOI
db2.datasource.username=tibco_soi_user
db2.datasource.password=dbuser@123
```

## Ready to Run

You mentioned you've already:
- ✅ Created COMPARATOR_CONFIG table in TIBCO_MS
- ✅ Created COMPARATOR_HISTORY table in TIBCO_MS
- ✅ Inserted one record in COMPARATOR_CONFIG

Now you can run the application from IntelliJ:
1. Click the green play button next to `Main.main()`
2. Wait for startup to complete
3. Test with: `curl http://localhost:8080/api/compare/health`
4. Run comparison: `curl -X POST http://localhost:8080/api/compare`

## Verification Queries

```sql
-- Check configuration (run in TIBCO_MS)
SELECT * FROM COMPARATOR_CONFIG WHERE comparator_execution_status = 'N';

-- Check history after comparison (run in TIBCO_MS)
SELECT * FROM COMPARATOR_HISTORY ORDER BY created_at DESC LIMIT 10;

-- Count differences by service
SELECT service_id, COUNT(*) as diff_count
FROM COMPARATOR_HISTORY
GROUP BY service_id;
```

All set! The application is ready to run with the updated architecture.
