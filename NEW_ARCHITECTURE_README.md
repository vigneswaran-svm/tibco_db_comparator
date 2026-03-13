# Database Comparator - New Architecture

## Overview

This REST API service compares records from the same table across two MariaDB databases (TIBCO_MS and TIBCO_SOI), identifies differences, and stores results in a history table. The configuration is driven by a database table (`COMPARATOR_CONFIG`) rather than properties files.

## Architecture Changes

### Database-Driven Configuration
- **Old**: Configuration in `application.properties`
- **New**: Configuration in `COMPARATOR_CONFIG` table in TIBCO_SOI database

### Result Storage
- **Old**: Stored in `comparison_results` table
- **New**: Stored in `COMPARATOR_HISTORY` table with JSON values

### Databases
- **TIBCO_MS** (db1): Source database for comparison
- **TIBCO_SOI** (db2): Target database + stores config and history

## Database Tables

### COMPARATOR_CONFIG (in TIBCO_SOI)
Stores comparison configurations:

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key (auto-increment) |
| service_id | VARCHAR(100) | Service identifier |
| table_name | VARCHAR(128) | Table to compare |
| table_fields | TEXT | Comma-separated fields to compare |
| primary_fields | TEXT | Comma-separated primary key fields |
| comparator_execution_status | CHAR(1) | 'N' = enabled, 'Y' = disabled |
| start_date | DATETIME | Timestamp range start |
| end_date | DATETIME | Timestamp range end |
| created_at | TIMESTAMP | Record creation time |
| updated_at | TIMESTAMP | Record update time |

### COMPARATOR_HISTORY (in TIBCO_SOI)
Stores comparison results:

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key (auto-increment) |
| service_id | VARCHAR(100) | Service identifier |
| table_name | VARCHAR(128) | Table name |
| fields_name | TEXT | Comma-separated fields compared |
| comparator_execution_status | VARCHAR(10) | 'success' or 'failure' |
| soi_table_values | TEXT | JSON of SOI database values |
| ms_table_values | TEXT | JSON of MS database values |
| created_at | TIMESTAMP | Record creation time |
| updated_at | TIMESTAMP | Record update time |

## How It Works

1. **Configuration Lookup**: Service reads from `COMPARATOR_CONFIG` table
   - Filters by `comparator_execution_status = 'N'` (enabled configs)
   - Optionally filters by `service_id` and/or `table_name` from API request

2. **Data Fetching**: For each configuration:
   - Fetches records from specified table in both databases
   - Filters by timestamp range (`start_date` to `end_date`)
   - Selects only the fields specified in `table_fields` + `primary_fields`

3. **Comparison**: Compares records using:
   - **Primary key match**: Uses `primary_fields` to match records
   - **Field comparison**: Compares only `table_fields` values
   - **Difference detection**: Identifies missing records and field mismatches

4. **Result Storage**: Stores differences in `COMPARATOR_HISTORY`:
   - Saves full record values as JSON
   - Stores both MS and SOI values for comparison
   - Marks execution status as 'success'

## API Endpoints

### 1. Run Comparison
```
POST /api/compare
```

**Request Body (optional):**
```json
{
  "serviceName": "NOTIFICATION_SERVICE",
  "tableName": "notification_log"
}
```

**Response:**
```json
{
  "status": "SUCCESS",
  "message": "Comparison completed successfully",
  "serviceId": "NOTIFICATION_SERVICE",
  "tableName": "notification_log",
  "totalRecordsMsDb": 150,
  "totalRecordsSoiDb": 148,
  "totalDifferencesFound": 5,
  "recordsWithDifferences": 3,
  "configsProcessed": 1,
  "comparisonDate": "2024-01-15T10:30:00"
}
```

### 2. Get Comparison History
```
GET /api/compare/history?serviceId=NOTIFICATION_SERVICE&tableName=notification_log
```

**Response:**
```json
[
  {
    "id": 1,
    "serviceId": "NOTIFICATION_SERVICE",
    "tableName": "notification_log",
    "fieldsName": "notification_id,message,status",
    "comparatorExecutionStatus": "success",
    "msTableValues": "{\"notification_id\":\"123\",\"message\":\"Test\",\"status\":\"sent\"}",
    "soiTableValues": "{\"notification_id\":\"123\",\"message\":\"Test\",\"status\":\"pending\"}",
    "createdAt": "2024-01-15T10:30:00"
  }
]
```

### 3. Get Active Configurations
```
GET /api/compare/configs/active
```

**Response:**
```json
[
  {
    "id": 1,
    "serviceId": "NOTIFICATION_SERVICE",
    "tableName": "notification_log",
    "tableFields": "notification_id,message,status,recipient,created_at",
    "primaryFields": "notification_id",
    "comparatorExecutionStatus": "N",
    "startDate": "2024-01-01T00:00:00",
    "endDate": "2024-12-31T23:59:59"
  }
]
```

### 4. Get All Configurations
```
GET /api/compare/configs
```

### 5. Clear History
```
DELETE /api/compare/history?serviceId=NOTIFICATION_SERVICE
```

### 6. Health Check
```
GET /api/compare/health
```

## Setup Instructions

### 1. Database Configuration

Update `src/main/resources/application.properties`:

```properties
# Database 1 (TIBCO_MS - Source)
db1.datasource.url=jdbc:mariadb://localhost:3306/TIBCO_MS
db1.datasource.username=tibco_user
db1.datasource.password=your_password
db1.datasource.driver-class-name=org.mariadb.jdbc.Driver

# Database 2 (TIBCO_SOI - Target + Config + Results)
db2.datasource.url=jdbc:mariadb://localhost:3306/TIBCO_SOI
db2.datasource.username=tibco_soi_user
db2.datasource.password=your_password
db2.datasource.driver-class-name=org.mariadb.jdbc.Driver
```

### 2. Create Tables

Run these SQL scripts in **TIBCO_SOI** database:

```sql
-- Create COMPARATOR_CONFIG table
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

-- Create COMPARATOR_HISTORY table
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

### 3. Add Configuration

Insert a configuration record (see `sample-comparator-config.sql`):

```sql
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

### 4. Build and Run

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run
```

### 5. Test the API

```bash
# Health check
curl http://localhost:8080/api/compare/health

# View active configurations
curl http://localhost:8080/api/compare/configs/active

# Run comparison (all active configs)
curl -X POST http://localhost:8080/api/compare

# Run comparison for specific service
curl -X POST http://localhost:8080/api/compare \
  -H "Content-Type: application/json" \
  -d '{"serviceName": "NOTIFICATION_SERVICE"}'

# View history
curl http://localhost:8080/api/compare/history?serviceId=NOTIFICATION_SERVICE
```

## Usage Scenarios

### Scenario 1: Compare All Active Configs
```bash
curl -X POST http://localhost:8080/api/compare
```
Processes all configurations where `comparator_execution_status = 'N'`

### Scenario 2: Compare Specific Service
```bash
curl -X POST http://localhost:8080/api/compare \
  -H "Content-Type: application/json" \
  -d '{"serviceName": "NOTIFICATION_SERVICE"}'
```
Processes only configs for the specified service_id

### Scenario 3: Compare Specific Table
```bash
curl -X POST http://localhost:8080/api/compare \
  -H "Content-Type: application/json" \
  -d '{"tableName": "notification_log"}'
```
Processes only configs for the specified table_name

### Scenario 4: Compare Specific Service + Table
```bash
curl -X POST http://localhost:8080/api/compare \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "NOTIFICATION_SERVICE",
    "tableName": "notification_log"
  }'
```
Processes only the specific config matching both filters

## Timestamp-Based Comparison

The service filters records based on the `created_at` timestamp column in the source tables, using `start_date` and `end_date` from the configuration.

**SQL Query Pattern:**
```sql
SELECT notification_id, message, status, recipient, created_at
FROM notification_log
WHERE created_at >= '2024-01-01 00:00:00'
  AND created_at <= '2024-12-31 23:59:59'
```

This allows incremental comparisons by adjusting the date range in the configuration.

## Managing Configurations

### Enable/Disable Configurations

```sql
-- Disable a configuration
UPDATE COMPARATOR_CONFIG
SET comparator_execution_status = 'Y'
WHERE service_id = 'NOTIFICATION_SERVICE';

-- Enable a configuration
UPDATE COMPARATOR_CONFIG
SET comparator_execution_status = 'N'
WHERE service_id = 'NOTIFICATION_SERVICE';
```

### Update Date Range for Incremental Comparison

```sql
UPDATE COMPARATOR_CONFIG
SET start_date = '2024-02-01 00:00:00',
    end_date = '2024-02-29 23:59:59'
WHERE service_id = 'NOTIFICATION_SERVICE';
```

### View Comparison History

```sql
-- All differences
SELECT * FROM COMPARATOR_HISTORY;

-- Differences for specific service
SELECT * FROM COMPARATOR_HISTORY
WHERE service_id = 'NOTIFICATION_SERVICE';

-- Recent differences (last 24 hours)
SELECT * FROM COMPARATOR_HISTORY
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR);

-- Count differences by service
SELECT service_id, COUNT(*) as difference_count
FROM COMPARATOR_HISTORY
GROUP BY service_id;
```

## Key Features

✅ **Database-Driven Configuration** - No code changes needed for new comparisons
✅ **Multi-Service Support** - Configure multiple services and tables
✅ **Timestamp Filtering** - Incremental comparison based on date ranges
✅ **Flexible Field Selection** - Compare only specified fields
✅ **Composite Primary Keys** - Support for multi-column primary keys
✅ **JSON Storage** - Full record values stored as JSON for analysis
✅ **Enable/Disable** - Turn configs on/off without deletion
✅ **REST API** - Easy integration with other systems

## Troubleshooting

### No Configurations Found
```
{"status":"WARNING","message":"No active configurations found with status 'N'"}
```
**Solution**: Check if you have any configs with `comparator_execution_status = 'N'`

```sql
SELECT * FROM COMPARATOR_CONFIG WHERE comparator_execution_status = 'N';
```

### Table Not Found
Check application logs for detailed error. Ensure:
- Table exists in both TIBCO_MS and TIBCO_SOI
- Table name in config matches exactly (case-sensitive)

### Field Not Found
Ensure all fields in `table_fields` and `primary_fields` exist in the table schema.

### Connection Issues
Verify database credentials in `application.properties` and network connectivity.

## Performance Considerations

- **Large Tables**: Consider limiting date ranges for better performance
- **Field Selection**: Only specify fields you need to compare
- **Indexing**: Ensure `created_at` is indexed for faster timestamp filtering
- **Batch Processing**: Process configs in batches during off-peak hours

## Security

- Store database credentials securely (environment variables, secrets manager)
- Add authentication to API endpoints for production use
- Restrict database user permissions (read-only on TIBCO_MS)
- Encrypt sensitive data in COMPARATOR_HISTORY if needed

## Next Steps

1. Configure your databases in `application.properties`
2. Create the required tables in TIBCO_SOI
3. Insert your comparison configurations
4. Build and run the application
5. Test with the API endpoints
6. Monitor comparison history
7. Schedule periodic comparisons (optional)

For more information, see the original README.md and API_TESTING_GUIDE.md files.