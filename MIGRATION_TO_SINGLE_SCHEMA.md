# Migration Guide: Multi-Tenant to Single Schema Per Instance

## Summary of Changes

The application has been migrated from a multi-tenant architecture (one instance serving multiple practices with dynamic schema switching) to a **single schema per instance** architecture (one instance per practice).

## What Changed

### Disabled Components

The following components have been **disabled** (commented out with `@Component`):

1. **`TenantResolutionFilter`** - Previously resolved tenant from Keycloak groups and headers
2. **`TenantContextInterceptor`** - Previously set tenant context from request headers
3. **`TenantSchemaInterceptor`** - Previously switched database schemas per operation
4. **`TenantIdentifierResolver`** - Previously resolved Hibernate tenant identifiers
5. **`MultiTenantConnectionProvider`** - Previously provided per-tenant database connections
6. **`MultiTenantDataSourceConfig`** - Previously configured multi-tenant datasources
7. **`MultiTenantJpaConfig`** - Previously configured multi-tenant JPA

### New Components

1. **`SingleSchemaConfig`** - Configures a single schema at application startup
   - Creates schema if it doesn't exist
   - Sets database default search path
   - Configured via `CIYEX_SCHEMA_NAME` environment variable

### Configuration Changes

**application.yml** changes:

```yaml
# OLD (removed)
ciyex:
  tenant:
    auto-init: false
    auto-create-on-request: true

# NEW
ciyex:
  schema:
    name: ${CIYEX_SCHEMA_NAME:public}
```

## Migration Steps

### Step 1: Identify Your Practices

List all practices currently served by your multi-tenant instance:

```sql
-- If using Keycloak groups
SELECT name FROM keycloak_group WHERE path LIKE '/Tenants/%';

-- If using org_config table
SELECT org_id, org_name FROM org_config;

-- List existing schemas
SELECT schema_name FROM information_schema.schemata 
WHERE schema_name LIKE 'practice_%';
```

### Step 2: Prepare Infrastructure

For each practice, you'll need:

1. **Separate deployment** (container, VM, or process)
2. **Unique port or domain** for routing
3. **Environment configuration** with schema name

### Step 3: Deploy Instances

#### Option A: Docker Compose

Create a `docker-compose.yml` with one service per practice:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: ciyexdb
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"

  ciyex-practice1:
    image: ciyex:latest
    environment:
      - CIYEX_SCHEMA_NAME=practice_1
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/ciyexdb
      - SPRING_DATASOURCE_USERNAME=postgres
      - SPRING_DATASOURCE_PASSWORD=postgres
    ports:
      - "8081:8080"
    depends_on:
      - postgres

  ciyex-practice2:
    image: ciyex:latest
    environment:
      - CIYEX_SCHEMA_NAME=practice_2
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/ciyexdb
      - SPRING_DATASOURCE_USERNAME=postgres
      - SPRING_DATASOURCE_PASSWORD=postgres
    ports:
      - "8082:8080"
    depends_on:
      - postgres
```

Deploy:
```bash
docker-compose up -d
```

#### Option B: Kubernetes

Create separate deployments for each practice:

```yaml
# practice-1.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ciyex-practice1
  labels:
    app: ciyex
    practice: practice1
spec:
  replicas: 2
  selector:
    matchLabels:
      app: ciyex
      practice: practice1
  template:
    metadata:
      labels:
        app: ciyex
        practice: practice1
    spec:
      containers:
      - name: ciyex
        image: ciyex:latest
        env:
        - name: CIYEX_SCHEMA_NAME
          value: "practice_1"
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: db-config
              key: url
        ports:
        - containerPort: 8080
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
---
apiVersion: v1
kind: Service
metadata:
  name: ciyex-practice1
spec:
  selector:
    app: ciyex
    practice: practice1
  ports:
  - port: 80
    targetPort: 8080
  type: ClusterIP
```

Deploy:
```bash
kubectl apply -f practice-1.yaml
kubectl apply -f practice-2.yaml
```

#### Option C: Traditional Deployment

Run separate Java processes:

```bash
# Practice 1
nohup java -jar ciyex.jar \
  --server.port=8081 \
  --ciyex.schema.name=practice_1 \
  > practice1.log 2>&1 &

# Practice 2
nohup java -jar ciyex.jar \
  --server.port=8082 \
  --ciyex.schema.name=practice_2 \
  > practice2.log 2>&1 &
```

### Step 4: Configure Load Balancer

Route traffic to the appropriate instance based on domain or path.

#### Nginx Configuration

```nginx
# Domain-based routing
upstream practice1 {
    server localhost:8081;
}

upstream practice2 {
    server localhost:8082;
}

server {
    listen 80;
    server_name practice1.example.com;
    
    location / {
        proxy_pass http://practice1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

server {
    listen 80;
    server_name practice2.example.com;
    
    location / {
        proxy_pass http://practice2;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Reload nginx:
```bash
sudo nginx -t
sudo systemctl reload nginx
```

### Step 5: Update API Clients

Remove tenant-related headers from API clients:

**Before:**
```javascript
// Old multi-tenant API call
fetch('https://api.example.com/api/patients', {
  headers: {
    'Authorization': `Bearer ${token}`,
    'X-Tenant-Name': 'Qiaben Health'  // ❌ Remove this
  }
})
```

**After:**
```javascript
// New single-schema API call
fetch('https://practice1.example.com/api/patients', {
  headers: {
    'Authorization': `Bearer ${token}`
    // No tenant header needed ✅
  }
})
```

### Step 6: Update Frontend Configuration

Update your frontend to use practice-specific URLs:

```javascript
// config.js
const API_URLS = {
  practice1: 'https://practice1.example.com',
  practice2: 'https://practice2.example.com'
};

// Determine which practice based on domain or config
const currentPractice = window.location.hostname.includes('practice1') 
  ? 'practice1' 
  : 'practice2';

const API_BASE_URL = API_URLS[currentPractice];
```

### Step 7: Verify Each Instance

Test each instance independently:

```bash
# Test Practice 1
curl -H "Authorization: Bearer $TOKEN" \
  https://practice1.example.com/api/patients

# Test Practice 2
curl -H "Authorization: Bearer $TOKEN" \
  https://practice2.example.com/api/patients

# Check health endpoints
curl https://practice1.example.com/actuator/health
curl https://practice2.example.com/actuator/health
```

### Step 8: Monitor and Validate

1. **Check logs** for each instance:
   ```bash
   # Look for successful schema initialization
   grep "Successfully configured instance to use schema" practice1.log
   ```

2. **Verify database connections**:
   ```sql
   -- Check active connections per schema
   SELECT schemaname, count(*) 
   FROM pg_stat_activity 
   WHERE datname = 'ciyexdb' 
   GROUP BY schemaname;
   ```

3. **Test data isolation**:
   - Create a patient in practice1
   - Verify it doesn't appear in practice2
   - Verify it appears in practice1

## Rollback Plan

If you need to rollback to multi-tenant:

1. **Re-enable components** by uncommenting `@Component` annotations:
   - `TenantResolutionFilter`
   - `TenantContextInterceptor`
   - `TenantSchemaInterceptor`
   - `TenantIdentifierResolver`

2. **Revert application.yml**:
   ```yaml
   ciyex:
     tenant:
       auto-init: false
       auto-create-on-request: true
   ```

3. **Disable SingleSchemaConfig**:
   - Comment out `@Configuration` annotation

4. **Redeploy** single multi-tenant instance

5. **Update clients** to include `X-Tenant-Name` header again

## Testing Checklist

- [ ] Each instance starts successfully
- [ ] Schema is created automatically
- [ ] Database search path is set correctly
- [ ] API endpoints respond without tenant headers
- [ ] Data is isolated between instances
- [ ] Authentication works for each instance
- [ ] Flyway migrations run correctly
- [ ] Audit logs are generated properly
- [ ] File uploads work correctly
- [ ] Reports generate successfully
- [ ] Scheduled jobs run independently

## Common Issues and Solutions

### Issue: Schema not found

**Symptoms**: `ERROR: schema "practice_1" does not exist`

**Solution**:
```sql
-- Manually create schema
CREATE SCHEMA practice_1;
GRANT ALL ON SCHEMA practice_1 TO postgres;
```

### Issue: Tables not found

**Symptoms**: `ERROR: relation "patient" does not exist`

**Solution**: Run Flyway migrations for the schema
```bash
CIYEX_SCHEMA_NAME=practice_1 ./gradlew flywayMigrate
```

### Issue: Wrong schema being used

**Symptoms**: Data from wrong practice appearing

**Solution**: Check environment variable
```bash
# Verify environment variable
echo $CIYEX_SCHEMA_NAME

# Check application logs
grep "Initializing single schema configuration" application.log
```

### Issue: Connection pool exhaustion

**Symptoms**: `Unable to acquire JDBC Connection`

**Solution**: Adjust connection pool settings per instance
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
```

## Performance Considerations

### Before (Multi-Tenant)
- Shared connection pool across all tenants
- Schema switching overhead on each request
- Complex tenant resolution logic
- Potential contention between tenants

### After (Single Schema)
- Dedicated connection pool per practice
- No schema switching overhead
- Simpler request processing
- Independent resource allocation

### Recommended Settings Per Instance

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

## Cost Considerations

### Infrastructure Costs
- **More instances**: Higher compute costs
- **Better isolation**: Worth the cost for healthcare data
- **Easier scaling**: Can scale per practice needs
- **Simpler operations**: Lower operational costs

### Optimization Tips
1. Use smaller instances for smaller practices
2. Share database server across instances
3. Use auto-scaling per practice
4. Monitor resource usage per instance

## Next Steps

1. **Monitor** each instance for 24-48 hours
2. **Optimize** resource allocation based on usage
3. **Document** which instance serves which practice
4. **Update** runbooks and operational procedures
5. **Train** team on new architecture
6. **Plan** for future practice onboarding

## Support

If you encounter issues during migration:

1. Check application logs for each instance
2. Verify database schema configuration
3. Test API endpoints independently
4. Review this migration guide
5. Consult `SINGLE_SCHEMA_SETUP.md` for detailed configuration

## Conclusion

The single-schema-per-instance architecture provides:
- ✅ Better data isolation
- ✅ Simpler architecture
- ✅ Easier troubleshooting
- ✅ Independent scaling
- ✅ Improved security

This is the recommended approach for healthcare applications requiring strong compliance and data isolation.
