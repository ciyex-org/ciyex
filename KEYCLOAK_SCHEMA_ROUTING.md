# Keycloak Schema Routing Guide

## Overview

Your application now automatically routes database queries to the correct PostgreSQL schema based on the `schema_name` attribute stored in Keycloak tenant groups. This provides flexible multi-tenant data isolation without hardcoding schema names in your application.

## How It Works

### 1. **Keycloak Group Attributes**

Each tenant group in Keycloak (under `/Tenants/`) can have a `schema_name` attribute that specifies which database schema to use:

```
Keycloak Group Structure:
├── Tenants
│   ├── practice_1
│   │   └── attributes: { "schema_name": "practice_1_db", "org_id": "1" }
│   ├── hinisoft
│   │   └── attributes: { "schema_name": "hinisoft_production", "org_id": "2" }
│   └── demo_clinic
│       └── attributes: { "schema_name": "demo_schema", "org_id": "3" }
```

### 2. **Request Flow**

```
User Request with JWT Token
    ↓
TenantResolutionFilter extracts:
  - User's groups from JWT
  - Group attributes (including schema_name)
  - Tenant name
    ↓
RequestContext populated with:
  - tenantName: "practice_1"
  - schemaName: "practice_1_db" (from Keycloak)
    ↓
JPA Interceptor / DataSource Wrapper:
  - Reads schemaName from RequestContext
  - Executes: SET search_path TO "practice_1_db", public
    ↓
All SQL queries automatically use the correct schema
```

### 3. **Components Involved**

#### **RequestContext** (`/dto/integration/RequestContext.java`)
- ThreadLocal storage for request-scoped data
- Now includes `schemaName` field from Keycloak

#### **TenantResolutionFilter** (`/filter/TenantResolutionFilter.java`)
- Extracts JWT token from Authorization header
- Calls `KeycloakAuthService.extractGroupAttributesFromToken()`
- Extracts `schema_name` from group attributes
- Stores in `RequestContext.schemaName`

#### **JpaSchemaInterceptor** (`/interceptor/JpaSchemaInterceptor.java`)
- Hibernate interceptor that runs before every query
- Reads `schemaName` from RequestContext
- Executes `SET search_path TO <schema>, public`

#### **TenantAwareDataSource** (`/config/TenantAwareJpaConfig.java`)
- Wraps DataSource to set schema on connection creation
- Also uses `schemaName` from RequestContext

## Setup Instructions

### 1. Configure Keycloak Groups

Add the `schema_name` attribute to your tenant groups in Keycloak:

**Via Keycloak Admin Console:**
1. Navigate to Groups → Tenants → [Your Tenant Group]
2. Go to "Attributes" tab
3. Add attribute:
   - Key: `schema_name`
   - Value: `your_schema_name`

**Via REST API:**
```bash
# Get admin token
TOKEN=$(curl -X POST "http://keycloak:8080/realms/ciyex/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=admin-cli" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password" | jq -r '.access_token')

# Update group with schema_name attribute
curl -X PUT "http://keycloak:8080/admin/realms/ciyex/groups/{group-id}" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "attributes": {
      "schema_name": ["practice_1_db"],
      "org_id": ["1"]
    }
  }'
```

**Via TenantSchemaService:**
```java
@Autowired
private TenantSchemaService tenantSchemaService;

// This will create the schema and update Keycloak group attribute
tenantSchemaService.ensureSchemaForTenant("practice_1");
```

### 2. JWT Token Configuration

Ensure your Keycloak client is configured to include group attributes in JWT tokens:

1. Go to Clients → [Your Client] → Client Scopes
2. Add a mapper for group attributes:
   - Mapper Type: `Group Membership`
   - Token Claim Name: `groups`
   - Full group path: ON
   - Add to ID token: ON
   - Add to access token: ON
   - Add to userinfo: ON

3. Add a protocol mapper for group attributes:
   - Mapper Type: `User Attribute`
   - User Attribute: `group_attributes`
   - Token Claim Name: `group_attributes`
   - Claim JSON Type: `JSON`

### 3. Database Schema Creation

The application automatically creates schemas if they don't exist. However, you can pre-create them:

```sql
-- Create schema
CREATE SCHEMA IF NOT EXISTS practice_1_db;

-- Grant permissions
GRANT ALL ON SCHEMA practice_1_db TO your_app_user;
GRANT ALL ON ALL TABLES IN SCHEMA practice_1_db TO your_app_user;
```

## Usage Examples

### Making API Requests

**Single Tenant User:**
```bash
# User belongs to only one tenant - no header needed
curl -X GET "http://localhost:8080/api/patients" \
  -H "Authorization: Bearer <jwt-token>"
```

**Multi-Tenant User:**
```bash
# User belongs to multiple tenants - must specify X-Tenant-Name
curl -X GET "http://localhost:8080/api/patients" \
  -H "Authorization: Bearer <jwt-token>" \
  -H "X-Tenant-Name: practice_1"
```

**Admin User (Full Access):**
```bash
# Admin with full access - must always specify X-Tenant-Name
curl -X GET "http://localhost:8080/api/patients" \
  -H "Authorization: Bearer <jwt-token>" \
  -H "X-Tenant-Name: hinisoft"
```

### Programmatic Access

```java
@Service
public class PatientService {
    
    @Autowired
    private PatientRepository patientRepository;
    
    public List<Patient> getAllPatients() {
        // No need to manually set schema!
        // The JPA interceptor automatically routes to the correct schema
        // based on RequestContext.schemaName
        return patientRepository.findAll();
    }
}
```

## Fallback Behavior

If `schema_name` is not found in Keycloak group attributes, the system falls back to generating a schema name from the tenant name:

```java
// Keycloak group attribute (preferred)
schema_name = "custom_schema_name"

// Fallback (auto-generated from tenant name)
schema_name = tenantName.toLowerCase()
                        .replaceAll("[^a-z0-9]+", "_")
                        .replaceAll("^_|_$", "")

// Example:
// tenantName: "Practice One" → schemaName: "practice_one"
```

## Debugging

Enable debug logging to see schema routing in action:

```yaml
# application.yml
logging:
  level:
    com.qiaben.ciyex.filter.TenantResolutionFilter: DEBUG
    com.qiaben.ciyex.interceptor.JpaSchemaInterceptor: DEBUG
    com.qiaben.ciyex.config.TenantAwareJpaConfig: DEBUG
```

**Log Output:**
```
DEBUG TenantResolutionFilter : Using schema_name from Keycloak group attributes: practice_1_db
DEBUG TenantResolutionFilter : Resolved tenant: practice_1, schema: practice_1_db, orgId: 1 for URI: /api/patients
DEBUG JpaSchemaInterceptor   : JPA Interceptor: Set search_path to: practice_1_db, public (from Keycloak: true)
```

## Security Considerations

1. **Schema Isolation**: Each tenant's data is isolated in separate PostgreSQL schemas
2. **Automatic Validation**: The filter validates tenant access before setting schema
3. **SQL Injection Protection**: All schema names are sanitized and quoted
4. **Immutable Schema Assignment**: Schema name from Keycloak is trusted source

## Migration from Hardcoded Schemas

If you're migrating from hardcoded schema names:

1. **Add schema_name attributes** to all existing Keycloak tenant groups
2. **Verify schema names** match your existing database schemas
3. **Test with one tenant** before rolling out to all
4. **Monitor logs** to ensure correct schema routing

## Troubleshooting

### Issue: "No schema_name in Keycloak group attributes"
**Solution:** Add the `schema_name` attribute to your Keycloak group or let the system auto-generate it.

### Issue: "Schema does not exist"
**Solution:** The system auto-creates schemas. Ensure your database user has CREATE SCHEMA permission.

### Issue: "X-Tenant-Name header is required"
**Solution:** User has access to multiple tenants. Specify which tenant to use via the header.

### Issue: Queries returning empty results
**Solution:** Check that:
1. The schema exists and has data
2. The correct schema is being set (check debug logs)
3. The user has access to the specified tenant

## Performance Notes

- Schema switching happens at the connection level (very fast)
- No query rewriting or parsing overhead
- PostgreSQL `search_path` is efficient for multi-schema queries
- Connection pooling works normally with this approach
