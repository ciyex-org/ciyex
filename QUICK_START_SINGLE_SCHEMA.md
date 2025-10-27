# Quick Start: Single Schema Configuration

## TL;DR

Each application instance now serves **one practice** using **one schema**. No more tenant switching.

## Running Locally

### Option 1: Default (Public Schema)
```bash
./gradlew bootRun
```

### Option 2: Specific Schema
```bash
CIYEX_SCHEMA_NAME=practice_1 ./gradlew bootRun
```

### Option 3: Multiple Instances (Different Terminals)
```bash
# Terminal 1 - Practice 1
CIYEX_SCHEMA_NAME=practice_1 SERVER_PORT=8081 ./gradlew bootRun

# Terminal 2 - Practice 2
CIYEX_SCHEMA_NAME=practice_2 SERVER_PORT=8082 ./gradlew bootRun
```

## Environment Variables

| Variable | Description | Default | Example |
|----------|-------------|---------|---------|
| `CIYEX_SCHEMA_NAME` | Database schema to use | `public` | `practice_1` |
| `SERVER_PORT` | Application port | `8080` | `8081` |

## API Usage

### Before (Multi-Tenant) ❌
```bash
curl -H "Authorization: Bearer $TOKEN" \
     -H "X-Tenant-Name: Practice1" \
     http://localhost:8080/api/patients
```

### After (Single Schema) ✅
```bash
# No tenant header needed!
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8081/api/patients
```

## Docker

### Single Instance
```bash
docker run -e CIYEX_SCHEMA_NAME=practice_1 \
           -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/ciyexdb \
           -p 8081:8080 \
           ciyex:latest
```

### Docker Compose
```yaml
services:
  ciyex-practice1:
    image: ciyex:latest
    environment:
      - CIYEX_SCHEMA_NAME=practice_1
    ports:
      - "8081:8080"
```

## Database Setup

### Create Schema
```sql
CREATE SCHEMA practice_1;
GRANT ALL ON SCHEMA practice_1 TO postgres;
```

### Run Migrations
```bash
CIYEX_SCHEMA_NAME=practice_1 ./gradlew flywayMigrate
```

## Verification

### Check Logs
Look for this at startup:
```
INFO  SingleSchemaConfig - Successfully configured instance to use schema: practice_1
```

### Check Database
```sql
-- Current schema
SELECT current_schema();

-- Should show: practice_1

-- Search path
SHOW search_path;

-- Should show: practice_1, public
```

### Test API
```bash
# Health check
curl http://localhost:8081/actuator/health

# Get patients (requires auth)
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8081/api/patients
```

## Common Issues

### Schema Not Found
```
ERROR: schema "practice_1" does not exist
```
**Fix:** Create the schema manually or let the app create it automatically.

### Wrong Schema
```
ERROR: relation "patient" does not exist
```
**Fix:** Run Flyway migrations for the schema.

### Port Already in Use
```
Port 8080 is already in use
```
**Fix:** Use a different port: `SERVER_PORT=8081 ./gradlew bootRun`

## Configuration Files

### application.yml
```yaml
ciyex:
  schema:
    name: ${CIYEX_SCHEMA_NAME:public}
```

### application-dev.yml (Example)
```yaml
ciyex:
  schema:
    name: practice_dev
```

### application-prod.yml (Example)
```yaml
ciyex:
  schema:
    name: ${CIYEX_SCHEMA_NAME}  # Must be set via env var
```

## Development Workflow

### 1. Create New Practice Schema
```sql
CREATE SCHEMA practice_acme;
```

### 2. Run Migrations
```bash
CIYEX_SCHEMA_NAME=practice_acme ./gradlew flywayMigrate
```

### 3. Start Instance
```bash
CIYEX_SCHEMA_NAME=practice_acme SERVER_PORT=8083 ./gradlew bootRun
```

### 4. Test
```bash
curl http://localhost:8083/actuator/health
```

## Production Deployment

### Kubernetes
```yaml
env:
- name: CIYEX_SCHEMA_NAME
  value: "practice_1"
```

### Docker
```bash
docker run -e CIYEX_SCHEMA_NAME=practice_1 ciyex:latest
```

### Traditional
```bash
export CIYEX_SCHEMA_NAME=practice_1
java -jar ciyex.jar
```

## Key Points

✅ **One instance = One practice = One schema**  
✅ **No tenant headers needed**  
✅ **Schema set at startup via environment variable**  
✅ **Complete data isolation between instances**  
✅ **Simpler configuration and deployment**

## Need More Info?

- **Full Setup Guide**: `SINGLE_SCHEMA_SETUP.md`
- **Migration Guide**: `MIGRATION_TO_SINGLE_SCHEMA.md`
- **Summary**: `TENANT_REMOVAL_SUMMARY.md`

## Questions?

**Q: Can one instance serve multiple practices?**  
A: No, that's the old multi-tenant architecture. Each instance serves one practice now.

**Q: Do I need to set X-Tenant-Name header?**  
A: No, tenant headers are no longer used.

**Q: How do I switch between practices?**  
A: Use different URLs/domains that route to different instances.

**Q: What if I need multi-tenant back?**  
A: See rollback procedure in `MIGRATION_TO_SINGLE_SCHEMA.md`

**Q: Can I use the public schema?**  
A: Yes, it's the default if you don't set `CIYEX_SCHEMA_NAME`.
