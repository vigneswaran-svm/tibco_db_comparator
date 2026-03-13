# Quick Start Guide

This guide will help you get the Database Comparison REST API up and running quickly.

## Prerequisites

1. Java 17 or higher installed
2. Maven 3.6+ installed
3. Two MariaDB databases accessible
4. Basic knowledge of REST APIs

## Step 1: Configure Database Connections

Edit `src/main/resources/application.properties`:

```properties
# Database 1 (Source)
db1.datasource.url=jdbc:mariadb://your-host:3306/your-database1
db1.datasource.username=your-username
db1.datasource.password=your-password

# Database 2 (Target + Results)
db2.datasource.url=jdbc:mariadb://your-host:3306/your-database2
db2.datasource.username=your-username
db2.datasource.password=your-password

# Table and fields to compare
comparison.table.name=users
comparison.mandatory.fields=id,name,email,status
comparison.primary.key.field=id
```

## Step 2: Build the Project

```bash
cd /Users/vigneswaranviswakethu/IdeaProjects/tibco_db_comparator
mvn clean install
```

## Step 3: Run the Application

```bash
mvn spring-boot:run
```

Wait for the message: `Started Main in X seconds`

## Step 4: Test the API

### Option 1: Using cURL

```bash
# Health check
curl http://localhost:8080/api/compare/health

# Run comparison with default configuration
curl -X POST http://localhost:8080/api/compare

# View results
curl http://localhost:8080/api/compare/results
```

### Option 2: Using a REST Client (Postman, Insomnia, etc.)

**Compare Records:**
- Method: POST
- URL: `http://localhost:8080/api/compare`
- Body (optional):
```json
{
  "tableName": "users",
  "mandatoryFields": ["id", "name", "email"],
  "primaryKeyField": "id",
  "clearPreviousResults": true
}
```

**Get Results:**
- Method: GET
- URL: `http://localhost:8080/api/compare/results?tableName=users`

## Step 5: Set Up Test Data (Optional)

If you want to test with sample data:

1. Create two test databases:
```sql
CREATE DATABASE database1;
CREATE DATABASE database2;
```

2. Run the sample test data script from `src/main/resources/sample-test-data.sql`:
```bash
# For database1
mysql -u your-username -p database1 < src/main/resources/sample-test-data.sql

# For database2 (modify the script to insert into database2)
mysql -u your-username -p database2 < src/main/resources/sample-test-data.sql
```

3. Update `application.properties` with the test database configuration:
```properties
comparison.table.name=users
comparison.mandatory.fields=id,name,email,status
comparison.primary.key.field=id
```

4. Run the comparison and check results.

## Understanding the Results

After running a comparison, you'll get a response like:

```json
{
  "status": "SUCCESS",
  "message": "Comparison completed successfully",
  "tableName": "users",
  "totalRecordsDb1": 5,
  "totalRecordsDb2": 5,
  "totalDifferencesFound": 6,
  "recordsWithDifferences": 4,
  "fieldsCompared": ["id", "name", "email", "status"],
  "comparisonDate": "2024-01-15T10:30:00"
}
```

This tells you:
- Total records in each database
- How many field differences were found
- How many records have at least one difference
- Which fields were compared
- When the comparison was performed

The detailed differences are stored in the `comparison_results` table in database 2.

## Querying Stored Results

Get all results:
```bash
curl http://localhost:8080/api/compare/results
```

Get results for a specific table:
```bash
curl http://localhost:8080/api/compare/results?tableName=users
```

Get results for a specific record:
```bash
curl http://localhost:8080/api/compare/results/users/123
```

## Clear Old Results

Before running a new comparison, you might want to clear old results:

```bash
# Clear all results
curl -X DELETE http://localhost:8080/api/compare/results

# Clear results for specific table
curl -X DELETE http://localhost:8080/api/compare/results?tableName=users
```

Or use the `clearPreviousResults` flag in the comparison request:
```json
{
  "tableName": "users",
  "clearPreviousResults": true
}
```

## Common Issues

### Port Already in Use
If port 8080 is already in use, change it in `application.properties`:
```properties
server.port=8081
```

### Database Connection Failed
- Verify database credentials
- Check if databases are running
- Verify network connectivity
- Check firewall settings

### Table Not Found
- Verify the table exists in both databases
- Check table name spelling (case-sensitive)
- Make sure you're connected to the correct databases

### Field Not Found
- Verify all fields in `comparison.mandatory.fields` exist in the table
- Check field name spelling (case-sensitive)

## Next Steps

1. Customize the `application.properties` for your specific use case
2. Explore the REST API endpoints (see README.md for full documentation)
3. Integrate the API into your workflow
4. Consider scheduling periodic comparisons
5. Set up monitoring and alerts for detected differences

## Support

For detailed documentation, see [README.md](README.md)

For issues or questions, check the application logs at `logs/application.log`
