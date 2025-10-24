# Environment Configuration Guide

## Quick Reference

| Environment | Profile | Auto-Create Schema | Use Case |
|-------------|---------|-------------------|----------|
| **Local** | `local` | ✅ Enabled | Development, testing new features |
| **Test** | `test` | ✅ Enabled | Automated tests, QA testing |
| **Production** | `prod` | ❌ Disabled | Live production environment |

## Configuration Files

### application.yml (Base Configuration)
```yaml
ciyex:
  tenant:
    auto-create-on-request: true  # Default: enabled
```

### application-local.yml
```yaml
ciyex:
  env: local
  tenant:
    auto-create-on-request: true  # ✅ Auto-create enabled
```

### application-test.yml
```yaml
ciyex:
  env: test
  tenant:
    auto-create-on-request: true  # ✅ Auto-create enabled
```

### application-prod.yml
```yaml
ciyex:
  env: prod
  tenant:
    auto-create-on-request: false  # ❌ Auto-create DISABLED
```

## Running the Application

### Local Development
```bash
# Using Maven
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Using JAR
java -jar ciyex.jar --spring.profiles.active=local

# Using environment variable
export SPRING_PROFILES_ACTIVE=local
java -jar ciyex.jar
```

### Test Environment
```bash
java -jar ciyex.jar --spring.profiles.active=test
```

### Production
```bash
# Standard production (auto-create disabled)
java -jar ciyex.jar --spring.profiles.active=prod

# Override for initial deployment (enable auto-create temporarily)
java -jar ciyex.jar --spring.profiles.active=prod \
  -Dciyex.tenant.auto-create-on-request=true
```

## Environment Variables

You can override any configuration using environment variables:

```bash
# Override auto-create flag
export CIYEX_TENANT_AUTO_CREATE=false

# Override Keycloak admin credentials
export KEYCLOAK_ADMIN_USERNAME=admin
export KEYCLOAK_ADMIN_PASSWORD=secure-password

# Set active profile
export SPRING_PROFILES_ACTIVE=prod
```

## Docker Deployment

### Local/Test
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/ciyex.jar app.jar
ENV SPRING_PROFILES_ACTIVE=local
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Production
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/ciyex.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
ENV CIYEX_TENANT_AUTO_CREATE=false
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## Kubernetes ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: ciyex-config
data:
  SPRING_PROFILES_ACTIVE: "prod"
  CIYEX_TENANT_AUTO_CREATE: "false"
  KEYCLOAK_AUTH_SERVER_URL: "https://keycloak.example.com"
  KEYCLOAK_REALM: "master"
```

## Behavior by Environment

### Local/Test (Auto-Create Enabled)

**When tenant schema doesn't exist:**
1. ✅ Automatically creates schema
2. ✅ Runs Flyway migrations
3. ✅ Updates Keycloak with schema_name
4. ✅ Request proceeds normally

**Log Output:**
```
INFO  TenantSchemaService - Schema 'new_clinic' does not exist for tenant 'New Clinic', creating...
INFO  TenantSchemaService - Schema created: new_clinic
INFO  TenantSchemaService - Flyway migrations completed for schema: new_clinic
INFO  TenantSchemaService - Successfully updated Keycloak group 'New Clinic' with schema_name: new_clinic
```

### Production (Auto-Create Disabled)

**When tenant schema doesn't exist:**
1. ❌ Throws RuntimeException
2. ❌ Request fails with 500 error
3. ❌ Schema must be created manually

**Log Output:**
```
ERROR TenantSchemaService - Schema 'new_clinic' does not exist for tenant 'New Clinic' and auto-create is disabled
```

**Error Response:**
```json
{
  "error": "Internal Server Error",
  "message": "Schema 'new_clinic' does not exist for tenant 'New Clinic'. Auto-creation is disabled. Please create the schema manually."
}
```

## Production Schema Creation Workflow

### Option 1: Manual Creation

1. **Create schema in database:**
   ```sql
   CREATE SCHEMA new_clinic;
   ```

2. **Run migrations:**
   ```bash
   flyway migrate -schemas=new_clinic
   ```

3. **Update Keycloak:**
   - Add attribute `schema_name: new_clinic` to tenant group

### Option 2: Temporary Auto-Create

1. **Deploy with auto-create enabled:**
   ```bash
   CIYEX_TENANT_AUTO_CREATE=true java -jar app.jar --spring.profiles.active=prod
   ```

2. **Trigger schema creation:**
   - Make API request to tenant
   - Schema auto-created

3. **Redeploy with auto-create disabled:**
   ```bash
   CIYEX_TENANT_AUTO_CREATE=false java -jar app.jar --spring.profiles.active=prod
   ```

## Troubleshooting

### Check Current Configuration

Add this to your controller to verify settings:

```java
@Value("${ciyex.tenant.auto-create-on-request}")
private boolean autoCreate;

@GetMapping("/config")
public Map<String, Object> getConfig() {
    return Map.of(
        "autoCreateOnRequest", autoCreate,
        "activeProfile", System.getProperty("spring.profiles.active")
    );
}
```

### Verify Active Profile

```bash
# Check logs on startup
grep "The following profiles are active" application.log

# Expected output:
# The following profiles are active: prod
```

### Test Auto-Create Behavior

```bash
# In local/test - should succeed and create schema
curl -H "Authorization: Bearer $TOKEN" \
     -H "X-Tenant-Name: New Clinic" \
     http://localhost:8080/api/patients

# In prod - should fail with error
curl -H "Authorization: Bearer $TOKEN" \
     -H "X-Tenant-Name: New Clinic" \
     https://prod.example.com/api/patients
```

## Best Practices

✅ **DO:**
- Use `local` profile for development
- Use `test` profile for automated tests
- Use `prod` profile for production
- Create schemas manually in production
- Test schema creation in lower environments first

❌ **DON'T:**
- Enable auto-create in production without careful consideration
- Rely on auto-create for production deployments
- Skip testing schema creation in lower environments
- Forget to update Keycloak with schema_name attribute
