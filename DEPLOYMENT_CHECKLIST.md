# Deployment Checklist

Use this checklist to ensure the Database Comparison REST API is properly configured and deployed.

## Pre-Deployment

### Environment Setup
- [ ] Java 17 or higher installed
  ```bash
  java -version
  ```
- [ ] Maven 3.6+ installed
  ```bash
  mvn -version
  ```
- [ ] Access to both MariaDB databases
- [ ] Network connectivity between application and databases verified

### Database Setup
- [ ] Database 1 (source) is accessible
- [ ] Database 2 (target) is accessible
- [ ] User has SELECT permissions on Database 1
- [ ] User has SELECT, INSERT, CREATE permissions on Database 2
- [ ] Target table exists in both databases
- [ ] Target table has same structure in both databases
- [ ] Mandatory fields exist in both database tables
- [ ] Primary key field is defined and populated

### Configuration
- [ ] Updated `application.properties` with Database 1 connection details
  - URL
  - Username
  - Password
- [ ] Updated `application.properties` with Database 2 connection details
  - URL
  - Username
  - Password
- [ ] Set correct table name in `comparison.table.name`
- [ ] Set correct fields in `comparison.mandatory.fields` (comma-separated)
- [ ] Set correct primary key in `comparison.primary.key.field`
- [ ] Reviewed and adjusted server port if needed (default: 8080)
- [ ] Reviewed and adjusted logging levels if needed

## Build and Test

### Build
- [ ] Build project successfully
  ```bash
  mvn clean install
  ```
- [ ] No compilation errors
- [ ] All dependencies downloaded
- [ ] Build creates JAR file in `target/` directory

### Local Testing
- [ ] Start application
  ```bash
  mvn spring-boot:run
  ```
- [ ] Application starts without errors
- [ ] Check startup logs for configuration confirmation
- [ ] Database connections established successfully
- [ ] comparison_results table created in Database 2

### API Testing
- [ ] Health check responds successfully
  ```bash
  curl http://localhost:8080/api/compare/health
  ```
- [ ] Run test comparison
  ```bash
  curl -X POST http://localhost:8080/api/compare
  ```
- [ ] Comparison completes without errors
- [ ] Results are stored in comparison_results table
- [ ] Retrieve results via API
  ```bash
  curl http://localhost:8080/api/compare/results
  ```
- [ ] Results match expected differences
- [ ] Test with custom configuration
- [ ] Test clearing results
- [ ] Test all API endpoints (see API_TESTING_GUIDE.md)

## Data Validation

### Sample Data Testing (Optional)
- [ ] Load sample test data from `sample-test-data.sql`
- [ ] Run comparison
- [ ] Verify 5 records with differences detected
- [ ] Verify 6 field differences found
- [ ] Check results in database:
  ```sql
  SELECT * FROM database2.comparison_results;
  ```
- [ ] Results match expected differences documented in sample-test-data.sql

### Production Data Testing
- [ ] Run comparison on actual production tables (read-only test)
- [ ] Verify results make sense
- [ ] Check for unexpected differences
- [ ] Validate difference counts
- [ ] Spot-check specific records
- [ ] Verify no duplicate results
- [ ] Confirm timestamp is correct

## Performance Testing

- [ ] Test with representative data volume
- [ ] Measure comparison time for full table
- [ ] Monitor memory usage during comparison
- [ ] Verify application doesn't crash with large datasets
- [ ] Check database 2 disk space for results storage
- [ ] Test concurrent requests (if multiple users)
- [ ] Review application logs for any warnings

## Production Deployment

### Security
- [ ] Review database credentials security
- [ ] Consider using environment variables for sensitive data
- [ ] Add authentication/authorization if needed (future enhancement)
- [ ] Review network security (firewall rules, VPN, etc.)
- [ ] Consider HTTPS for API endpoints (if exposed externally)

### Configuration
- [ ] Create production-specific application.properties
- [ ] Use production database credentials
- [ ] Adjust logging level (INFO or WARN for production)
- [ ] Configure log file location
- [ ] Set appropriate server port
- [ ] Configure JVM memory settings if needed

### Deployment
- [ ] Copy JAR file to production server
- [ ] Copy application.properties to production server
- [ ] Set up as system service (systemd, Windows service, etc.)
- [ ] Configure startup on boot
- [ ] Set up log rotation
- [ ] Configure monitoring

### Startup
- [ ] Start application in production environment
  ```bash
  java -jar tibco_db_comparator-1.0-SNAPSHOT.jar
  ```
- [ ] Verify startup without errors
- [ ] Check health endpoint
- [ ] Run test comparison
- [ ] Verify results

## Monitoring and Maintenance

### Monitoring Setup
- [ ] Set up application monitoring
- [ ] Configure log monitoring
- [ ] Set up alerts for errors
- [ ] Monitor disk space (for results storage)
- [ ] Monitor database connections
- [ ] Monitor API response times
- [ ] Set up uptime monitoring

### Documentation
- [ ] Document production configuration
- [ ] Document database credentials location
- [ ] Document troubleshooting procedures
- [ ] Document comparison schedule (if automated)
- [ ] Train users on API usage
- [ ] Create runbook for common issues

### Maintenance
- [ ] Schedule regular result cleanup
  ```bash
  curl -X DELETE http://localhost:8080/api/compare/results?tableName=old_table
  ```
- [ ] Plan for log file cleanup
- [ ] Schedule database maintenance
- [ ] Plan for backup of comparison_results table
- [ ] Set up regular health checks
- [ ] Review and update mandatory fields as schema changes

## Post-Deployment

### Verification
- [ ] Verify all API endpoints accessible
- [ ] Run full comparison test
- [ ] Verify results accuracy
- [ ] Check application logs
- [ ] Verify database connections stable
- [ ] Test error handling (invalid table, missing fields, etc.)

### User Acceptance
- [ ] Demo to stakeholders
- [ ] Validate results with business users
- [ ] Confirm comparison logic meets requirements
- [ ] Get sign-off on accuracy
- [ ] Provide user training
- [ ] Share API documentation

### Documentation
- [ ] Update README.md with production details
- [ ] Document any configuration changes
- [ ] Record known issues or limitations
- [ ] Document support contacts
- [ ] Update runbook

## Rollback Plan

In case of issues:
- [ ] Document current production state
- [ ] Keep backup of previous version
- [ ] Document rollback procedure
- [ ] Test rollback in non-production environment
- [ ] Have rollback decision criteria defined

## Future Enhancements (Optional)

Consider these for future iterations:
- [ ] Add authentication/authorization
- [ ] Add pagination for large result sets
- [ ] Add scheduled/automated comparisons
- [ ] Add email notifications for differences
- [ ] Add web UI for easier interaction
- [ ] Add comparison history and trends
- [ ] Add support for multiple table comparisons
- [ ] Add export functionality (CSV, Excel)
- [ ] Add filtering and search in results
- [ ] Add comparison of schema changes

## Quick Reference

### Start Application
```bash
mvn spring-boot:run
# or
java -jar target/tibco_db_comparator-1.0-SNAPSHOT.jar
```

### Stop Application
```bash
# Ctrl+C if running in foreground
# Or find process and kill
ps aux | grep tibco_db_comparator
kill <PID>
```

### View Logs
```bash
tail -f logs/application.log
```

### Quick Test
```bash
# Health check
curl http://localhost:8080/api/compare/health

# Run comparison
curl -X POST http://localhost:8080/api/compare

# View results
curl http://localhost:8080/api/compare/results
```

### Check Results in Database
```sql
USE database2;
SELECT COUNT(*) FROM comparison_results;
SELECT * FROM comparison_results ORDER BY comparison_date DESC LIMIT 10;
```

## Support Contacts

- Development Team: [Add contact info]
- Database Admin: [Add contact info]
- Operations Team: [Add contact info]

## Change Log

| Date | Version | Changes | Deployed By |
|------|---------|---------|-------------|
| YYYY-MM-DD | 1.0.0 | Initial deployment | [Name] |

---

**Note**: Check off each item as you complete it. Keep this checklist for future reference and updates.
