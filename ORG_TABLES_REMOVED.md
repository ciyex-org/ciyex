# Org and OrgConfig Tables/Classes Removed

## Summary

Removed all Org and OrgConfig database tables, entities, repositories, services, DTOs, and controllers. Organization information is now loaded from Keycloak tenant group attributes instead of database tables.

## Files Removed

### Entities
- ✅ `/src/main/java/com/qiaben/ciyex/entity/Org.java`
- ✅ `/src/main/java/com/qiaben/ciyex/entity/OrgConfig.java`

### Repositories
- ✅ `/src/main/java/com/qiaben/ciyex/repository/OrgRepository.java`
- ✅ `/src/main/java/com/qiaben/ciyex/repository/OrgConfigRepository.java`

### Services
- ✅ `/src/main/java/com/qiaben/ciyex/service/OrgService.java`
- ✅ `/src/main/java/com/qiaben/ciyex/service/OrgConfigService.java`

### Controllers
- ✅ `/src/main/java/com/qiaben/ciyex/controller/OrgController.java`
- ✅ `/src/main/java/com/qiaben/ciyex/controller/OrgConfigController.java`

### DTOs
- ✅ `/src/main/java/com/qiaben/ciyex/dto/OrgDto.java`

## Files Created

### New Service
- ✅ `/src/main/java/com/qiaben/ciyex/service/KeycloakOrgService.java`
  - Loads organization information from Keycloak group attributes
  - Provides methods to get org name, schema name, storage type, FHIR URL
  - Can update group attributes

## How It Works Now

### Before (Database):
```java
// Load org from database
Org org = orgRepository.findById(orgId);
String orgName = org.getName();
String storageType = org.getConfig().getStorageType();
```

### After (Keycloak):
```java
// Load org from Keycloak group attributes
@Autowired
private KeycloakOrgService keycloakOrgService;

String tenantGroup = "/Tenants/CareWell";
String orgName = keycloakOrgService.getOrgName(tenantGroup);
String storageType = keycloakOrgService.getStorageType(tenantGroup);
String schemaName = keycloakOrgService.getSchemaName(tenantGroup);
String fhirUrl = keycloakOrgService.getFhirServerUrl(tenantGroup);

// Get all attributes
Map<String, Object> allAttributes = keycloakOrgService.getAllAttributes(tenantGroup);
```

## Keycloak Group Attributes

Organization configuration is now stored as Keycloak group attributes:

### Example Group Structure:
```
/Tenants/CareWell
  Attributes:
    - org_name: "CareWell Medical Center"
    - schema_name: "practice_1"
    - storage_type: "fhir"
    - fhir_server_url: "https://fhir.carewell.com"
    - org_id: "1"
    - address: "123 Main St"
    - phone: "+1-555-0100"
    - email: "contact@carewell.com"
    - ... (any custom attributes)
```

## KeycloakOrgService API

### Get Methods:
```java
// Get specific attributes
String orgName = keycloakOrgService.getOrgName(tenantGroup);
String schemaName = keycloakOrgService.getSchemaName(tenantGroup);
String storageType = keycloakOrgService.getStorageType(tenantGroup);
String fhirUrl = keycloakOrgService.getFhirServerUrl(tenantGroup);

// Get all attributes
Map<String, Object> config = keycloakOrgService.getAllAttributes(tenantGroup);
String customValue = (String) config.get("custom_attribute");
```

### Update Methods:
```java
// Update attributes
Map<String, Object> updates = new HashMap<>();
updates.put("org_name", "New Name");
updates.put("fhir_server_url", "https://new-fhir.com");
keycloakOrgService.updateOrgAttributes(tenantGroup, updates);
```

## Files That Need Updates

The following files reference Org/OrgConfig and need to be updated:

### High Priority:
1. **UserOrgRole.java** - References `Org` entity
2. **OrganizationAuthService.java** - May reference Org
3. **OrgIntegrationConfigProvider.java** - Provides org config
4. **PatientService.java** - May reference org
5. **Any service using OrgRepository or OrgConfigRepository**

### Search for References:
```bash
# Find all references to Org entity
grep -r "import.*entity.Org" src/main/java/

# Find all references to OrgRepository
grep -r "OrgRepository" src/main/java/

# Find all references to OrgConfig
grep -r "OrgConfig" src/main/java/

# Find all references to OrgDto
grep -r "OrgDto" src/main/java/
```

## Migration Steps

### 1. Update UserOrgRole Entity
Remove the `@ManyToOne` relationship to `Org` entity:
```java
// Before:
@ManyToOne
@JoinColumn(name = "org_id")
private Org org;

// After: Store orgId or tenant group name directly
private Long orgId;
private String tenantGroup;  // e.g., "/Tenants/CareWell"
```

### 2. Update OrganizationAuthService
Replace database queries with Keycloak lookups:
```java
@Autowired
private KeycloakOrgService keycloakOrgService;

// Use Keycloak instead of database
String orgName = keycloakOrgService.getOrgName(tenantGroup);
```

### 3. Update OrgIntegrationConfigProvider
Load config from Keycloak instead of database:
```java
@Autowired
private KeycloakOrgService keycloakOrgService;

public String getStorageTypeForCurrentOrg() {
    String tenantGroup = getCurrentTenantGroup();
    return keycloakOrgService.getStorageType(tenantGroup);
}
```

### 4. Update Services Using Org
Replace all `orgRepository` calls with `keycloakOrgService` calls.

## Database Changes

### Tables to Drop:
```sql
-- Drop org_config table
DROP TABLE IF EXISTS org_config CASCADE;

-- Drop org table
DROP TABLE IF EXISTS org CASCADE;

-- Update user_org_role table (if needed)
-- ALTER TABLE user_org_role DROP CONSTRAINT IF EXISTS fk_user_org_role_org;
-- ALTER TABLE user_org_role DROP COLUMN IF EXISTS org_id;
-- ALTER TABLE user_org_role ADD COLUMN tenant_group VARCHAR(255);
```

## Benefits

### ✅ Single Source of Truth
- Organization data stored in Keycloak only
- No data synchronization needed between Keycloak and database

### ✅ Simplified Architecture
- Removed 9 files (entities, repositories, services, controllers, DTOs)
- Less code to maintain

### ✅ Centralized Management
- Manage all organization config in Keycloak admin console
- No need for separate org management API

### ✅ Flexible Attributes
- Can add any custom attributes to Keycloak groups
- No database schema changes needed

## Next Steps

1. ✅ Remove Org and OrgConfig files (DONE)
2. ✅ Create KeycloakOrgService (DONE)
3. ⏳ Update UserOrgRole entity
4. ⏳ Update OrganizationAuthService
5. ⏳ Update OrgIntegrationConfigProvider
6. ⏳ Update all services using OrgRepository
7. ⏳ Test compilation
8. ⏳ Test application startup
9. ⏳ Drop database tables
10. ⏳ Update documentation

## Testing

After updates, test:
1. User authentication with tenant groups
2. Organization config loading from Keycloak
3. Tenant context switching
4. FHIR integration (if using storage_type attribute)
5. All API endpoints that previously used Org data

## Rollback Plan

If needed, the removed files are in git history:
```bash
# View removed files
git log --diff-filter=D --summary

# Restore a file
git checkout <commit-hash> -- path/to/file
```
