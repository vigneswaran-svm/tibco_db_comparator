# API Testing Guide

This guide provides ready-to-use commands and examples for testing the Database Comparison REST API.

## Prerequisites

- Application running on `http://localhost:8080`
- Two databases configured and accessible
- Test data loaded (optional)

## 1. Health Check

### cURL
```bash
curl http://localhost:8080/api/compare/health
```

### Expected Response
```
Database Comparison Service is running
```

---

## 2. Run Comparison (Default Configuration)

### cURL
```bash
curl -X POST http://localhost:8080/api/compare \
  -H "Content-Type: application/json"
```

### Expected Response
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

---

## 3. Run Comparison (Custom Configuration)

### cURL
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

### Request Body Explanation
- `tableName`: Override the default table name
- `mandatoryFields`: Specify which fields to compare
- `primaryKeyField`: Specify the primary key field for matching records
- `clearPreviousResults`: true = clear old results before comparison

---

## 4. Get All Comparison Results

### cURL
```bash
curl http://localhost:8080/api/compare/results
```

### Expected Response
```json
[
  {
    "id": 1,
    "recordId": "1",
    "tableName": "users",
    "fieldName": "email",
    "db1Value": "john.doe@example.com",
    "db2Value": "john.doe.updated@example.com",
    "comparisonDate": "2024-01-15T10:30:00"
  },
  {
    "id": 2,
    "recordId": "2",
    "tableName": "users",
    "fieldName": "status",
    "db1Value": "active",
    "db2Value": "inactive",
    "comparisonDate": "2024-01-15T10:30:00"
  }
]
```

---

## 5. Get Results for Specific Table

### cURL
```bash
curl "http://localhost:8080/api/compare/results?tableName=users"
```

---

## 6. Get Results for Specific Record

### cURL
```bash
curl http://localhost:8080/api/compare/results/users/123
```

### Path Parameters
- `users`: Table name
- `123`: Record ID

---

## 7. Clear All Comparison Results

### cURL
```bash
curl -X DELETE http://localhost:8080/api/compare/results
```

### Expected Response
```
Comparison results cleared successfully
```

---

## 8. Clear Results for Specific Table

### cURL
```bash
curl -X DELETE "http://localhost:8080/api/compare/results?tableName=users"
```

---

## Complete Testing Workflow

### Step 1: Verify Service is Running
```bash
curl http://localhost:8080/api/compare/health
```

### Step 2: Clear Old Results (Optional)
```bash
curl -X DELETE "http://localhost:8080/api/compare/results?tableName=users"
```

### Step 3: Run Comparison
```bash
curl -X POST http://localhost:8080/api/compare \
  -H "Content-Type: application/json" \
  -d '{
    "tableName": "users",
    "mandatoryFields": ["id", "name", "email", "status"],
    "primaryKeyField": "id",
    "clearPreviousResults": true
  }'
```

### Step 4: Review Results
```bash
curl "http://localhost:8080/api/compare/results?tableName=users"
```

### Step 5: Check Specific Record Differences
```bash
curl http://localhost:8080/api/compare/results/users/1
```

---

## Testing with Sample Data

If you loaded the sample test data from `sample-test-data.sql`, you should see:

### Expected Comparison Summary
- Total Records DB1: 5
- Total Records DB2: 5
- Records with Differences: 4
- Total Field Differences: 6

### Expected Differences

**Record ID 1:**
- Field: email
- DB1: john.doe@example.com
- DB2: john.doe.updated@example.com

**Record ID 2:**
- Field: status
- DB1: active
- DB2: inactive

**Record ID 4:**
- Field: email
  - DB1: alice.williams@example.com
  - DB2: alice@example.com
- Field: status
  - DB1: active
  - DB2: pending

**Record ID 5:**
- All fields: MISSING_RECORD in DB2

**Record ID 6:**
- All fields: MISSING_RECORD in DB1

---

## Using Postman

### Import Collection

Create a new collection in Postman with these requests:

#### 1. Health Check
- Method: GET
- URL: `http://localhost:8080/api/compare/health`

#### 2. Compare (Default)
- Method: POST
- URL: `http://localhost:8080/api/compare`
- Headers: Content-Type: application/json
- Body: (none or empty JSON)

#### 3. Compare (Custom)
- Method: POST
- URL: `http://localhost:8080/api/compare`
- Headers: Content-Type: application/json
- Body (raw JSON):
```json
{
  "tableName": "users",
  "mandatoryFields": ["id", "name", "email"],
  "primaryKeyField": "id",
  "clearPreviousResults": true
}
```

#### 4. Get All Results
- Method: GET
- URL: `http://localhost:8080/api/compare/results`

#### 5. Get Results by Table
- Method: GET
- URL: `http://localhost:8080/api/compare/results?tableName=users`

#### 6. Get Results by Record
- Method: GET
- URL: `http://localhost:8080/api/compare/results/users/1`

#### 7. Clear All Results
- Method: DELETE
- URL: `http://localhost:8080/api/compare/results`

#### 8. Clear Table Results
- Method: DELETE
- URL: `http://localhost:8080/api/compare/results?tableName=users`

---

## Direct Database Verification

After running a comparison, verify results directly in the database:

```sql
-- Connect to database 2
USE database2;

-- View all comparison results
SELECT * FROM comparison_results;

-- Count differences by table
SELECT table_name, COUNT(*) as difference_count
FROM comparison_results
GROUP BY table_name;

-- View differences for specific record
SELECT *
FROM comparison_results
WHERE record_id = '1'
ORDER BY field_name;

-- View differences by field
SELECT field_name, COUNT(*) as count
FROM comparison_results
WHERE table_name = 'users'
GROUP BY field_name;

-- View most recent comparison
SELECT *
FROM comparison_results
WHERE comparison_date >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
ORDER BY comparison_date DESC;
```

---

## Troubleshooting

### Connection Refused
```bash
curl: (7) Failed to connect to localhost port 8080: Connection refused
```
**Solution**: Verify the application is running with `mvn spring-boot:run`

### 404 Not Found
```bash
{"timestamp":"2024-01-15T10:30:00","status":404,"error":"Not Found"}
```
**Solution**: Check the URL path. Ensure it starts with `/api/compare`

### 500 Internal Server Error
```bash
{"status":"ERROR","message":"Comparison failed","error":"Table 'users' doesn't exist"}
```
**Solution**:
1. Verify table exists in both databases
2. Check table name spelling
3. Review application logs for detailed error

### Empty Results
```json
[]
```
**Solution**:
1. Run a comparison first
2. Check if data exists in both databases
3. Verify there are actual differences

---

## Performance Testing

### Test with Large Dataset
```bash
# Run comparison
time curl -X POST http://localhost:8080/api/compare

# Check response time in output
```

### Monitor Application Logs
```bash
tail -f logs/application.log
```

---

## Security Testing (Future)

When authentication is added, update requests to include:

```bash
curl -X POST http://localhost:8080/api/compare \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{...}'
```

---

## Automated Testing Script

Create a bash script `test-api.sh`:

```bash
#!/bin/bash

BASE_URL="http://localhost:8080/api/compare"

echo "1. Health Check..."
curl -s "$BASE_URL/health"
echo -e "\n"

echo "2. Clearing old results..."
curl -s -X DELETE "$BASE_URL/results?tableName=users"
echo -e "\n"

echo "3. Running comparison..."
RESULT=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "tableName": "users",
    "mandatoryFields": ["id", "name", "email", "status"],
    "primaryKeyField": "id",
    "clearPreviousResults": true
  }')
echo "$RESULT" | jq '.'
echo -e "\n"

echo "4. Fetching results..."
curl -s "$BASE_URL/results?tableName=users" | jq '.'
echo -e "\n"

echo "Testing complete!"
```

Make it executable:
```bash
chmod +x test-api.sh
./test-api.sh
```

---

## Summary

This guide covers:
- ✅ All REST endpoints
- ✅ Request/response examples
- ✅ Complete testing workflow
- ✅ Postman integration
- ✅ Database verification
- ✅ Troubleshooting
- ✅ Performance testing
- ✅ Automated testing script

For more details, see [README.md](README.md) and [QUICKSTART.md](QUICKSTART.md).
