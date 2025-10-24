# Tenant Schema Management with Keycloak

## Overview

The application now uses **tenant group names** from Keycloak to automatically generate and manage database schemas. When a request comes in with a tenant group, the system:

1. Generates a schema name from the tenant group name
2. Creates the schema and tables if they don't exist
3. Updates the Keycloak group with the `schema_name` attribute
4. Routes all database queries to that schema

## Schema Name Generation

Tenant group names are converted to valid PostgreSQL schema names:

```
"Qiaben Health" → "qiaben_health"
"MediPlus"      → "mediplus"
"CareWell"      → "carewell"
```

**Rules:**
- Convert to lowercase
- Replace non-alphanumeric characters with underscores
- Remove leading/trailing underscores

## Request Flow

### Single Tenant User

```
User: alice@example.com
Groups: ["/Tenants/Qiaben Health"]

Request → No X-Tenant-Name header needed
System automatically uses "Qiaben Health"
Schema: qiaben_health
```

### Multi-Tenant User

```
User: bob@example.com  
Groups: ["/Tenants/Qiaben Health", "/Tenants/MediPlus"]

Request → Must include X-Tenant-Name header
Example: X-Tenant-Name: Qiaben Health
Schema: qiaben_health
```

### Full Access User (Apps Group)

```
User: admin@example.com
Groups: ["/Apps/Ciyex"]

Request → Must include X-Tenant-Name header
Example: X-Tenant-Name: CareWell
Schema: carewell
```

## API Headers

### X-Tenant-Name (Required for multi-tenant/full-access users)

Specifies which tenant to access:

```http
GET /api/patients
Authorization: Bearer <jwt-token>
X-Tenant-Name: Qiaben Health
```

### X-Org-Id (Deprecated, but still supported)

The old `X-Org-Id` header is still supported for backward compatibility, but `X-Tenant-Name` is preferred.

## Automatic Schema Creation

When a request comes in for a tenant whose schema doesn't exist:

1. **Generate schema name** from tenant group name
2. **Create schema** in PostgreSQL
3. **Run Flyway migrations** from `db/migration/tenant/`
4. **Update Keycloak** group with `schema_name` attribute
5. **Route request** to the new schema

### Example Log Output

```
INFO  TenantSchemaService - Schema 'qiaben_health' does not exist for tenant 'Qiaben Health', creating...
INFO  TenantSchemaService - Schema created: qiaben_health
INFO  TenantSchemaService - Flyway migrations completed for schema: qiaben_health
INFO  TenantSchemaService - Successfully updated Keycloak group 'Qiaben Health' with schema_name: qiaben_health
DEBUG TenantResolutionFilter - Resolved tenant: Qiaben Health, schema: qiaben_health, orgId: 1 for URI: /api/patients
```

## Keycloak Group Attributes

After schema creation, the Keycloak group will have:

```json
{
  "name": "Qiaben Health",
  "attributes": {
    "org_id": ["1"],
    "org_name": ["Qiaben Health"],
    "schema_name": ["qiaben_health"],
    "address": ["123 Main St"],
    "city": ["Denver"],
    ...
  }
}
```

## Configuration

### application.yml

```yaml
ciyex:
  tenant:
    auto-init: false  # Auto-init at startup
    auto-create-on-request: true  # Auto-create when tenant is accessed

keycloak:
  auth-server-url: https://aran-stg.zpoa.com
  realm: master
  admin:
    username: ${KEYCLOAK_ADMIN_USERNAME:aran-admin}
    password: ${KEYCLOAK_ADMIN_PASSWORD:}
```

### Environment-Specific Configuration

**Local/Test (application-local.yml, application-test.yml):**
```yaml
ciyex:
  tenant:
    auto-create-on-request: true  # ✅ Enable auto-creation
```

**Production (application-prod.yml):**
```yaml
ciyex:
  tenant:
    auto-create-on-request: false  # ❌ Disable auto-creation
```

### Environment Variables

```bash
# Keycloak Admin Credentials
KEYCLOAK_ADMIN_USERNAME=aran-admin
KEYCLOAK_ADMIN_PASSWORD=your-secure-password

# Or override auto-create flag
CIYEX_TENANT_AUTO_CREATE=false
```

### Running with Different Profiles

```bash
# Local development (auto-create enabled)
java -jar app.jar --spring.profiles.active=local

# Test environment (auto-create enabled)
java -jar app.jar --spring.profiles.active=test

# Production (auto-create disabled)
java -jar app.jar --spring.profiles.active=prod
```

## Key Classes

### TenantSchemaService

- `generateSchemaName(tenantGroupName)` - Converts tenant name to schema name
- `schemaExists(schemaName)` - Checks if schema exists
- `createSchemaWithMigrations(schemaName)` - Creates schema and runs Flyway
- `updateKeycloakGroupAttribute(tenantGroupName, schemaName)` - Updates Keycloak
- `ensureSchemaForTenant(tenantGroupName)` - Main entry point

### TenantResolutionFilter

- Extracts tenant name from Keycloak groups or X-Tenant-Name header
- Calls `ensureSchemaForTenant()` to create schema if needed
- Sets tenant context for the request

### TenantAccessService

- `hasAccessToAllTenants(groups)` - Checks if user has Apps group access
- `getAccessibleTenants(groups)` - Returns list of accessible tenant names
- `hasAccessToTenant(groups, tenantName)` - Validates tenant access

## Migration from org_id to Tenant Names

### Before (org_id based)

```java
X-Org-Id: 1
Schema: practice_1
```

### After (tenant name based)

```java
X-Tenant-Name: Qiaben Health
Schema: qiaben_health
```

### Backward Compatibility

- `org_id` is still stored in Keycloak group attributes
- Old code using `org_id` will continue to work
- New code should use tenant names

## Error Responses

### Missing X-Tenant-Name (Multi-tenant user)

```json
{
  "error": "X-Tenant-Name header is required",
  "message": "You have access to multiple tenants. Please specify X-Tenant-Name header. Accessible tenants: Qiaben Health, MediPlus"
}
```

### Invalid Tenant Access

```json
{
  "error": "Access denied",
  "message": "You do not have access to tenant: CareWell"
}
```

### No Tenant Access

```json
{
  "error": "No tenant access",
  "message": "You do not have access to any tenants."
}
```

### Schema Does Not Exist (Production with auto-create disabled)

```json
{
  "error": "Internal Server Error",
  "message": "Schema 'new_clinic' does not exist for tenant 'New Clinic'. Auto-creation is disabled. Please create the schema manually."
}
```

## Production Schema Management

In production, automatic schema creation is **disabled** by default. Schemas must be created manually or through a controlled deployment process.

### Manual Schema Creation

1. **Connect to database:**
   ```bash
   psql -h prod-db.example.com -U postgres -d ciyexdb
   ```

2. **Create schema:**
   ```sql
   CREATE SCHEMA new_clinic;
   ```

3. **Run Flyway migrations:**
   ```bash
   flyway -schemas=new_clinic \
          -locations=filesystem:./db/migration/tenant \
          -url=jdbc:postgresql://prod-db.example.com:5432/ciyexdb \
          -user=postgres \
          migrate
   ```

4. **Update Keycloak group attribute:**
   - Go to Keycloak Admin Console
   - Navigate to: Groups → Tenants → New Clinic → Attributes
   - Add attribute: `schema_name` = `new_clinic`

### Automated Production Deployment

For controlled production deployments, you can temporarily enable auto-creation:

```bash
# Deploy with auto-create enabled for initial setup
CIYEX_TENANT_AUTO_CREATE=true java -jar app.jar --spring.profiles.active=prod

# After schemas are created, redeploy with auto-create disabled
CIYEX_TENANT_AUTO_CREATE=false java -jar app.jar --spring.profiles.active=prod
```

## Testing

### Create a new tenant

1. Create group in Keycloak: `/Tenants/New Clinic`
2. Assign user to the group
3. Make API request (no X-Tenant-Name needed if single tenant)
4. System automatically creates `new_clinic` schema
5. Keycloak group updated with `schema_name: new_clinic`

### Verify schema creation

```sql
SELECT schema_name FROM information_schema.schemata 
WHERE schema_name NOT IN ('pg_catalog', 'information_schema', 'public');
```

### Check Keycloak attributes

Go to: Keycloak Admin → Groups → Tenants → New Clinic → Attributes

Should see: `schema_name: new_clinic`

## Benefits

✅ **No database mapping needed** - Schema name derived from Keycloak group  
✅ **Multi-application support** - Same tenant groups work across apps  
✅ **Automatic provisioning** - Schemas created on-demand  
✅ **Centralized management** - All tenant data in Keycloak  
✅ **Human-readable** - Schema names match tenant names  
✅ **Scalable** - No manual schema creation required
