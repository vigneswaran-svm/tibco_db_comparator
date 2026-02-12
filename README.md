# Database Comparison REST API

A Spring Boot REST API service that compares records from the same table across two different MariaDB databases, identifies differences based on configurable mandatory fields, and stores the results in a result table.

## Features

- Connect to 2 MariaDB databases simultaneously
- Compare records from the same table across both databases
- Configurable mandatory fields for comparison
- Store comparison results in database 2
- REST API endpoints for triggering comparisons and querying results
- Support for missing records detection
- Field-by-field difference tracking

## Technology Stack

- Java 17
- Spring Boot 3.2.1
- Spring Data JPA
- MariaDB
- Maven
- Lombok

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Two MariaDB databases with the same table structure
- Network access to both databases

## Configuration

Update `src/main/resources/application.properties` with your database connection details:

```properties
# Database 1 (Source)
db1.datasource.url=jdbc:mariadb://localhost:3306/database1
db1.datasource.username=user1
db1.datasource.password=pass1

# Database 2 (Target + Results)
db2.datasource.url=jdbc:mariadb://localhost:3306/database2
db2.datasource.username=user2
db2.datasource.password=pass2

# Comparison Configuration
comparison.table.name=your_table_name
comparison.mandatory.fields=id,field1,field2,field3
comparison.primary.key.field=id
```

### Configuration Parameters

- `db1.datasource.*`: Connection details for the first database (source)
- `db2.datasource.*`: Connection details for the second database (target + results storage)
- `comparison.table.name`: The table name to compare (used as default)
- `comparison.mandatory.fields`: Comma-separated list of fields to compare
- `comparison.primary.key.field`: The primary key field used to match records

## Building the Project

```bash
mvn clean install
```

## Running the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## API Endpoints

### 1. Compare Records

**Endpoint:** `POST /api/compare`

**Description:** Compares records from the same table in both databases.

**Request Body (Optional):**
```json
{
  "tableName": "users",
  "mandatoryFields": ["id", "name", "email"],
  "primaryKeyField": "id",
  "clearPreviousResults": true
}
```

**Response:**
```json
{
  "status": "SUCCESS",
  "message": "Comparison completed successfully",
  "tableName": "users",
  "totalRecordsDb1": 100,
  "totalRecordsDb2": 98,
  "totalDifferencesFound": 15,
  "recordsWithDifferences": 5,
  "fieldsCompared": ["id", "name", "email"],
  "comparisonDate": "2024-01-15T10:30:00"
}
```

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/compare \
  -H "Content-Type: application/json" \
  -d '{
    "tableName": "users",
    "mandatoryFields": ["id", "name", "email"],
    "primaryKeyField": "id",
    "clearPreviousResults": true
  }'
```

### 2. Get Comparison Results

**Endpoint:** `GET /api/compare/results`

**Query Parameters:**
- `tableName` (optional): Filter results by table name

**Response:**
```json
[
  {
    "id": 1,
    "recordId": "123",
    "tableName": "users",
    "fieldName": "email",
    "db1Value": "old@example.com",
    "db2Value": "new@example.com",
    "comparisonDate": "2024-01-15T10:30:00"
  }
]
```

**cURL Example:**
```bash
curl http://localhost:8080/api/compare/results?tableName=users
```

### 3. Get Results by Record ID

**Endpoint:** `GET /api/compare/results/{tableName}/{recordId}`

**Path Parameters:**
- `tableName`: The table name
- `recordId`: The record ID

**cURL Example:**
```bash
curl http://localhost:8080/api/compare/results/users/123
```

### 4. Clear Comparison Results

**Endpoint:** `DELETE /api/compare/results`

**Query Parameters:**
- `tableName` (optional): Clear results for specific table only

**cURL Example:**
```bash
curl -X DELETE http://localhost:8080/api/compare/results?tableName=users
```

### 5. Health Check

**Endpoint:** `GET /api/compare/health`

**cURL Example:**
```bash
curl http://localhost:8080/api/compare/health
```

## Database Schema

The application automatically creates the `comparison_results` table in database 2:

```sql
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
);
```

## How It Works

1. **Configuration**: The application reads database connection details and comparison configuration from `application.properties`.

2. **Data Fetching**: When a comparison is triggered, the service fetches records from the specified table in both databases, selecting only the mandatory fields plus the primary key.

3. **Comparison Logic**:
   - Records are matched using the primary key field
   - Each mandatory field is compared between the two databases
   - Differences are identified for:
     - Records present in DB1 but missing in DB2
     - Records present in DB2 but missing in DB1
     - Field values that differ between matching records

4. **Result Storage**: All identified differences are stored in the `comparison_results` table in database 2, with details about:
   - Which record (record ID)
   - Which table
   - Which field
   - The value in database 1
   - The value in database 2
   - When the comparison was performed

5. **Result Retrieval**: The API provides endpoints to query the stored comparison results, filtered by table name, record ID, or date range.

## Error Handling

The application handles various error scenarios:
- Table doesn't exist in one or both databases
- Mandatory fields don't exist in the table
- Empty tables
- Database connection failures
- Different record counts in both databases

Error responses include detailed error messages and appropriate HTTP status codes.

## Example Usage Workflow

1. **Configure your databases** in `application.properties`

2. **Start the application**:
   ```bash
   mvn spring-boot:run
   ```

3. **Run a comparison**:
   ```bash
   curl -X POST http://localhost:8080/api/compare
   ```

4. **View the results**:
   ```bash
   curl http://localhost:8080/api/compare/results
   ```

5. **Query specific record differences**:
   ```bash
   curl http://localhost:8080/api/compare/results/users/123
   ```

6. **Clear old results before new comparison**:
   ```bash
   curl -X DELETE http://localhost:8080/api/compare/results?tableName=users
   ```

## Project Structure

```
src/
├── main/
│   ├── java/org/example/
│   │   ├── Main.java                          # Spring Boot application entry point
│   │   ├── config/
│   │   │   ├── DatabaseConfig.java            # Dual database configuration
│   │   │   └── Db2RepositoryConfig.java       # Repository configuration
│   │   ├── controller/
│   │   │   └── ComparisonController.java      # REST API endpoints
│   │   ├── dto/
│   │   │   ├── ComparisonRequest.java         # Request DTO
│   │   │   └── ComparisonResponse.java        # Response DTO
│   │   ├── model/
│   │   │   └── db2/
│   │   │       └── ComparisonResult.java      # JPA entity for results
│   │   ├── repository/
│   │   │   └── ComparisonResultRepository.java # JPA repository
│   │   └── service/
│   │       └── ComparisonService.java         # Business logic
│   └── resources/
│       ├── application.properties              # Configuration
│       └── schema.sql                          # Database schema
└── test/
```

## Logging

The application uses SLF4J with Logback for logging. Log levels can be configured in `application.properties`:

```properties
logging.level.org.example=DEBUG
logging.level.org.springframework.jdbc=DEBUG
```

## Future Enhancements

- Add pagination for large result sets
- Add filtering by date range
- Add authentication/authorization
- Add scheduled comparison jobs
- Add support for comparing multiple tables in one request
- Add email notifications for detected differences
- Add comparison history and analytics
- Add support for different database types (PostgreSQL, MySQL, etc.)

## Troubleshooting

### Connection Issues
- Verify database connection details in `application.properties`
- Ensure databases are accessible from the application server
- Check firewall rules and network connectivity

### Table Not Found
- Verify the table exists in both databases
- Check table name spelling and case sensitivity

### Field Not Found
- Verify all mandatory fields exist in the table
- Check field name spelling and case sensitivity

### Memory Issues with Large Tables
- Consider adding pagination to the comparison service
- Increase JVM heap size: `java -Xmx2g -jar app.jar`

## License

This project is for internal use.
