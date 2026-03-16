# Database Comparator - New Architecture

## Overview

This REST API service compares records from the same table across two MariaDB databases (TIBCO_MS and TIBCO_SOI), identifies differences, and stores results in a history table. 
The configuration is driven by a database table (`COMPARATOR_CONFIG`) rather than properties files.

## Architecture Changes

## Table column updated
- Added column name 'soi_service_name_whitelist' and  'where_date_field' in COMPARATOR_CONFIG
- Updated column name 'ms_service_name_whitelist' instead of 'service_name_whitelist' in COMPARATOR_CONFIG table

## Alter DB Script
ALTER TABLE `TIBCO_MS`.`COMPARATOR_CONFIG`
ADD COLUMN `soi_service_name_whitelist` MEDIUMTEXT NOT NULL AFTER `ms_service_name_whitelist`,
ADD COLUMN `where_date_field` VARCHAR(50) NULL AFTER `updated_at`,
CHANGE COLUMN `service_name_whitelist` `ms_service_name_whitelist` MEDIUMTEXT NULL DEFAULT NULL ;

## Functional and Logic changes
## Current behaviour 
- The column name 'service_name_whitelist' used to filter the where condition 'updated_by' in both DB.

## Expected behaviour
- The column name 'updated_by'should refer the below column in both DB.
- Fetch records based on 'soi_service_name_whitelist' service name values pass into when refer in SOI_DB.
- Fetch records based on 'ms_service_name_whitelist' service name values pass into when refer in MS_DB.
- The new column name 'where_date_field' should use in where condition dynamically based on config table values because not the same column name for all the table 
- remain all logic should be same after the fetch records.

## Changes
- Need to update where condition 'where_date_field' column value.
- Filter the where condition used with 'updated_by' each DB have using with different service name.


## Not in Use this logic just add it for backup
## Additional Logic
## Context / Problem Statement
- There is a service name mismatch between SOI_DB and MS_DB when comparing records by service name. The updated_by column in SOI_DB uses SOI-style service names (e.g., CORRESPONDENCEREQUEST), while MS_DB uses MS-style service names (e.g., soi-correspondence-service). These need to be mapped before comparison.
- Build a mapping HashMap using the whitelist configuration fields:
    Keys → values from soi_service_name_whitelist (split by ,)
    Values → values from ms_service_name_whitelist (split by ,), maintaining the same positional order
- Fetch records from SOI_DB and for each row:
    Read the updated_by column value (e.g., CORRESPONDENCEREQUEST)
    Use it as a key to look up the corresponding MS service name from the HashMap
    Add the resolved MS service name to the list
- Compare records between SOI_DB and MS_DB using the resolved MS service name (not the raw updated_by value), so that both sides are on the same naming convention before comparison.
