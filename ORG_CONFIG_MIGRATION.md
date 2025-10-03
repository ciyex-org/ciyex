# OrgConfig Migration Guide

This document describes the migration of the `org_config` table from tenant schemas to the master schema.

## Overview

Previously, the `org_config` table was created in each tenant schema (`practice_{orgId}`). This migration moves it to the master schema (`public`) to:

1. **Simplify schema management** - Global configuration should be in the master schema
2. **Improve performance** - Avoid schema switching when accessing org configuration  
3. **Better data consistency** - Centralized organization configuration management

## What Changed

### Code Changes

1. **TenantSchemaInitializer.java**
   - Added `OrgConfig` to the list of master entities in `isTenantEntity()`
   - Removed JSONB column handling for `org_config` from tenant schema operations
   - `org_config` tables will no longer be created in new tenant schemas

2. **MasterSchemaInitializer.java**
   - Added `OrgConfig.class` to master schema entities
   - Added JSONB column type enforcement for `org_config.integrations` in master schema
   - `org_config` tables will now be created in the master schema

3. **OrgConfigService.java**
   - Added `ensureMasterSchema()` method to force operations to use the master schema
   - All CRUD operations now explicitly set schema to `public` before execution

4. **OrgIntegrationConfigProvider.java**
   - Added `ensureMasterSchema()` calls to ensure config lookups use the master schema
   - All integration config retrieval now works from the master schema

### New Migration Components

1. **Master Migration Scripts**
   - `db/migration/master/base/V5__move_org_config_to_master.sql`
   - `db/migration/master/local/V3__move_org_config_to_master.sql` 
   - `db/migration/master/stg/V5__move_org_config_to_master.sql`
   - `db/migration/master/prod/V5__move_org_config_to_master.sql`
   - Creates org_config table in master schema and migrates data from tenant schemas

2. **Tenant Migration Scripts**
   - `db/migration/tenant/base/V2__drop_org_config_from_tenant.sql`
   - `db/migration/tenant/local/V3__drop_org_config_from_tenant.sql`
   - `db/migration/tenant/stg/V2__drop_org_config_from_tenant.sql` 
   - `db/migration/tenant/prod/V2__drop_org_config_from_tenant.sql`
   - Drops org_config tables from tenant schemas after data migration

## Migration Process

### Automatic Migration via Flyway (Recommended)

The migration is handled automatically by Flyway during application startup:

1. **Master Migration**: Creates `org_config` table in master schema and migrates existing data
2. **Tenant Migration**: Drops `org_config` tables from tenant schemas  
3. **Order**: Master migrations run first, then tenant migrations

No manual intervention is required - just deploy the application with the new migration scripts.

### Migration Order

1. **V5__move_org_config_to_master.sql** (Master Schema)
   - Creates org_config table in public schema
   - Migrates all existing data from tenant schemas
   - Handles JSONB column type conversion
   - Runs first during deployment

2. **V2__drop_org_config_from_tenant.sql** (Tenant Schemas)  
   - Drops org_config table from each tenant schema
   - Runs after master migration is complete
   - Safe cleanup operation

### Migration Steps (What Happens)

1. **Master Migration (V5__move_org_config_to_master.sql)**:
   - Creates `org_config` table in public schema if not exists
   - Scans all tenant schemas (`practice_*`) for existing org_config data
   - Migrates data safely with conflict handling (ON CONFLICT DO NOTHING)
   - Ensures JSONB column type for integrations field
   - Logs migration progress and results

2. **Tenant Migration (V2__drop_org_config_from_tenant.sql)**:
   - Drops `org_config` table from each tenant schema
   - Uses `DROP TABLE IF EXISTS` for safe operation
   - Logs completion for each schema

3. **Automatic Execution**:
   - Master migrations run first during Flyway master schema migration
   - Tenant migrations run for each existing tenant schema
   - All handled by existing Flyway infrastructure

## Database Schema Changes

### Before Migration
```
┌─────────────────────┐
│   Master Schema     │
│     (public)        │
│                     │
│ ┌─────────────────┐ │
│ │ users           │ │
│ │ orgs            │ │
│ │ user_org_roles  │ │
│ └─────────────────┘ │
└─────────────────────┘

┌─────────────────────┐
│ Tenant Schema       │
│   (practice_1)      │
│                     │
│ ┌─────────────────┐ │
│ │ org_config      │ │  ← Was here
│ │ patients        │ │
│ │ appointments    │ │
│ │ ...             │ │
│ └─────────────────┘ │
└─────────────────────┘
```

### After Migration
```
┌─────────────────────┐
│   Master Schema     │
│     (public)        │
│                     │
│ ┌─────────────────┐ │
│ │ users           │ │
│ │ orgs            │ │
│ │ user_org_roles  │ │
│ │ org_config      │ │  ← Now here
│ └─────────────────┘ │
└─────────────────────┘

┌─────────────────────┐
│ Tenant Schema       │
│   (practice_1)      │
│                     │
│ ┌─────────────────┐ │
│ │ patients        │ │
│ │ appointments    │ │
│ │ encounters      │ │
│ │ ...             │ │
│ └─────────────────┘ │  (org_config removed)
└─────────────────────┘
```

## Impact Assessment

### Positive Impacts
- ✅ **No API changes** - All existing endpoints continue to work
- ✅ **No data loss** - All existing configuration is preserved
- ✅ **Better performance** - No schema switching needed for config access
- ✅ **Simplified deployments** - One place for organization configuration

### Considerations
- ⚠️ **Migration required** - One-time data migration needed
- ⚠️ **Deployment coordination** - Should be done during maintenance window
- ⚠️ **Rollback complexity** - Rolling back requires data migration in reverse

## Testing the Migration

### Before Migration
1. Document existing org_config records:
   ```sql
   -- Run for each tenant schema
   SET search_path TO practice_1;
   SELECT * FROM org_config;
   ```

### During Migration
1. Watch Flyway logs during application startup:
   ```
   INFO  - Migrating schema "public" to version "2 - move org config to master"
   INFO  - Starting migration of org_config from tenant schemas to master schema...
   INFO  - Migrated 1 org_config record(s) from schema practice_1 for org_id 1
   INFO  - Migration completed. Total records processed: 3
   ```

### After Migration  
1. Verify master schema has all records:
   ```sql
   SET search_path TO public;
   SELECT COUNT(*) FROM org_config;
   SELECT org_id, integrations FROM org_config ORDER BY org_id;
   ```

2. Confirm tenant schemas no longer have org_config tables:
   ```sql
   SET search_path TO practice_1;
   SELECT * FROM org_config; -- Should error: relation does not exist
   ```

3. Test application functionality:
   - Login and verify organization context works
   - Test integration configurations (Stripe, S3, etc.)
   - Verify document storage settings
   - Check all org-specific features

### Verification Queries

```sql
-- Count records by schema
SELECT 
    'public' as schema_name,
    COUNT(*) as org_config_count 
FROM public.org_config
UNION ALL
SELECT 
    schema_name,
    COALESCE((
        SELECT COUNT(*) 
        FROM information_schema.tables t2 
        WHERE t2.table_schema = s.schema_name 
        AND t2.table_name = 'org_config'
    ), 0) as table_exists
FROM information_schema.schemata s 
WHERE schema_name LIKE 'practice_%';

-- Verify data integrity
SELECT 
    org_id,
    CASE 
        WHEN integrations IS NULL THEN 'NULL'
        WHEN integrations::text = '{}' THEN 'EMPTY'
        ELSE 'HAS_DATA'
    END as integrations_status,
    jsonb_array_length(jsonb_object_keys(integrations)) as config_count
FROM public.org_config 
ORDER BY org_id;
```

## Troubleshooting

### Common Issues

1. **Migration fails with "OrgConfig already exists"**
   - This is expected behavior for existing records
   - Check logs to ensure all records are accounted for

2. **JSONB parsing errors**
   - Indicates corrupted integration data in tenant schemas
   - Check the source data and fix manually if needed

3. **Schema permission errors**
   - Ensure the application user has necessary permissions
   - Required: CREATE, ALTER, DROP on schemas and tables

4. **Service startup errors after migration**
   - Verify all services are using the updated code
   - Check that master schema contains expected tables

### Recovery Steps

If migration fails:

1. **Check logs** for specific error messages
2. **Verify database state** - what was migrated successfully?
3. **Manual cleanup** if needed:
   ```sql
   -- Remove partially migrated data if needed
   DELETE FROM public.org_config WHERE org_id = <specific_org_id>;
   ```
4. **Re-run migration** for specific organizations if needed

## Rollback Plan

If rollback is necessary:

1. **Stop the application**
2. **Migrate data back to tenant schemas**:
   ```sql
   -- For each organization
   SET search_path TO practice_<org_id>;
   CREATE TABLE IF NOT EXISTS org_config (LIKE public.org_config);
   INSERT INTO org_config SELECT * FROM public.org_config WHERE org_id = <org_id>;
   ```
3. **Deploy previous version** of the application
4. **Clean up master schema**:
   ```sql
   DROP TABLE IF EXISTS public.org_config;
   ```

## Post-Migration Verification

- [ ] All org_config records migrated successfully
- [ ] Application starts without errors  
- [ ] Organization login/switching works
- [ ] Integration configurations accessible
- [ ] Document storage settings work
- [ ] No tenant schema org_config tables remain (if cleanup was run)
- [ ] Performance improvements observed (fewer schema switches)

## Future Considerations

After this migration:

1. **New tenants** will not have org_config tables in their schemas
2. **All org configuration** happens in the master schema
3. **Schema switching** is only needed for tenant-specific data
4. **Integration configuration** is now centralized and more efficient