# Single Schema Per Instance Setup

## Overview

The application has been simplified to use **one schema per instance per practice**. Each deployed instance of the application serves a single practice and uses a dedicated database schema.

This replaces the previous multi-tenant architecture where one instance dynamically switched between multiple tenant schemas based on request headers.

## Architecture

### Previous Multi-Tenant Architecture (Removed)
- ❌ One instance served multiple practices
- ❌ Dynamic schema switching based on `X-Tenant-Name` or `X-Org-Id` headers
- ❌ Complex tenant resolution filters and interceptors
- ❌ Keycloak group-based tenant routing
- ❌ Multiple connection pools per tenant

### Current Single-Schema Architecture
- ✅ One instance per practice
- ✅ Single schema configured at startup via environment variable
- ✅ No runtime schema switching
- ✅ Simplified configuration and deployment
- ✅ Better isolation and security

## Configuration

### Environment Variables

Set the schema name for each instance using the `CIYEX_SCHEMA_NAME` environment variable:

```bash
# Practice 1 instance
CIYEX_SCHEMA_NAME=practice_1

# Practice 2 instance  
CIYEX_SCHEMA_NAME=practice_2

# Default (uses public schema)
# CIYEX_SCHEMA_NAME=public
```

### application.yml

```yaml
ciyex:
  schema:
    name: ${CIYEX_SCHEMA_NAME:public}  # Default to public schema
```

### Database Configuration

The schema is automatically created and configured at application startup:

1. **Schema Creation**: If the schema doesn't exist, it's created automatically
2. **Search Path**: The database default search path is set to: `{schema_name}, public`
3. **All Operations**: All JPA/Hibernate operations use the configured schema by default

## Deployment

### Docker Deployment

Each practice gets its own container with a specific schema:

```yaml
# docker-compose.yml
services:
  ciyex-practice1:
    image: ciyex:latest
    environment:
      - CIYEX_SCHEMA_NAME=practice_1
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/ciyexdb
    ports:
      - "8081:8080"

  ciyex-practice2:
    image: ciyex:latest
    environment:
      - CIYEX_SCHEMA_NAME=practice_2
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/ciyexdb
    ports:
      - "8082:8080"
```

### Kubernetes Deployment

Each practice gets its own deployment:

```yaml
# practice-1-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ciyex-practice1
spec:
  replicas: 2
  template:
    spec:
      containers:
      - name: ciyex
        image: ciyex:latest
        env:
        - name: CIYEX_SCHEMA_NAME
          value: "practice_1"
        - name: SPRING_DATASOURCE_URL
          value: "jdbc:postgresql://postgres:5432/ciyexdb"
---
apiVersion: v1
kind: Service
metadata:
  name: ciyex-practice1
spec:
  selector:
    app: ciyex-practice1
  ports:
  - port: 80
    targetPort: 8080
```

### Traditional Deployment

Run separate instances with different configurations:

```bash
# Practice 1
java -jar ciyex.jar \
  --server.port=8081 \
  --ciyex.schema.name=practice_1

# Practice 2
java -jar ciyex.jar \
  --server.port=8082 \
  --ciyex.schema.name=practice_2
```

## Database Setup

### Creating Schemas

Schemas are created automatically at startup, but you can also create them manually:

```sql
-- Create schema for practice 1
CREATE SCHEMA practice_1;

-- Create schema for practice 2
CREATE SCHEMA practice_2;

-- Grant permissions
GRANT ALL ON SCHEMA practice_1 TO postgres;
GRANT ALL ON SCHEMA practice_2 TO postgres;
```

### Running Migrations

Flyway migrations will run against the configured schema:

```bash
# For practice 1
CIYEX_SCHEMA_NAME=practice_1 ./gradlew flywayMigrate

# For practice 2
CIYEX_SCHEMA_NAME=practice_2 ./gradlew flywayMigrate
```

## Benefits

### Simplified Architecture
- No complex tenant resolution logic
- No runtime schema switching
- Easier to understand and maintain
- Fewer moving parts and potential bugs

### Better Isolation
- Complete process isolation between practices
- No risk of data leakage between tenants
- Independent scaling per practice
- Separate resource limits and monitoring

### Improved Security
- No shared memory or connection pools
- No header-based tenant selection (attack vector removed)
- Simpler authentication and authorization
- Easier to audit and comply with regulations

### Operational Benefits
- Independent deployments per practice
- Rolling updates without affecting other practices
- Easier troubleshooting and debugging
- Better resource allocation and monitoring
- Simpler disaster recovery

### Performance
- No overhead from tenant resolution
- No connection pool switching
- Optimized connection pooling per practice
- Better database query planning

## Migration from Multi-Tenant

### What Was Removed

The following components have been disabled/removed:

1. **Filters**
   - `TenantResolutionFilter` - Resolved tenant from headers/JWT

2. **Interceptors**
   - `TenantContextInterceptor` - Set tenant context from headers
   - `TenantSchemaInterceptor` - Switched schemas on database operations

3. **Multi-Tenant Components**
   - `TenantIdentifierResolver` - Hibernate tenant resolution
   - `MultiTenantConnectionProvider` - Per-tenant connection pools
   - `TenantRoutingDataSource` - Dynamic datasource routing
   - `TenantDataSourceProvider` - Tenant datasource factory

4. **Configuration**
   - `MultiTenantDataSourceConfig` - Multi-tenant datasource setup
   - `MultiTenantJpaConfig` - Multi-tenant JPA configuration
   - Tenant-related application.yml settings

### What Remains (But Unused)

These classes remain in the codebase but are disabled:
- `TenantContext` - Thread-local tenant storage (not used)
- `TenantSchemaService` - Schema management utilities (may be useful)
- `TenantAwareService` - Manual schema switching (not needed)

### Migration Steps

1. **Deploy separate instances** for each practice
2. **Set CIYEX_SCHEMA_NAME** environment variable for each instance
3. **Remove X-Tenant-Name headers** from API clients
4. **Update load balancer** to route by practice domain/path
5. **Test each instance** independently

## API Changes

### Before (Multi-Tenant)

Clients had to specify which tenant to access:

```bash
# Required header for multi-tenant users
curl -H "Authorization: Bearer $TOKEN" \
     -H "X-Tenant-Name: Qiaben Health" \
     https://api.example.com/api/patients
```

### After (Single Schema)

No tenant headers needed - each instance serves one practice:

```bash
# Practice 1 instance
curl -H "Authorization: Bearer $TOKEN" \
     https://practice1.example.com/api/patients

# Practice 2 instance
curl -H "Authorization: Bearer $TOKEN" \
     https://practice2.example.com/api/patients
```

## Load Balancing

Route requests to the appropriate instance based on domain or path:

### Domain-Based Routing

```nginx
# nginx.conf
server {
    server_name practice1.example.com;
    location / {
        proxy_pass http://ciyex-practice1:8080;
    }
}

server {
    server_name practice2.example.com;
    location / {
        proxy_pass http://ciyex-practice2:8080;
    }
}
```

### Path-Based Routing

```nginx
# nginx.conf
location /practice1/ {
    rewrite ^/practice1/(.*) /$1 break;
    proxy_pass http://ciyex-practice1:8080;
}

location /practice2/ {
    rewrite ^/practice2/(.*) /$1 break;
    proxy_pass http://ciyex-practice2:8080;
}
```

## Monitoring

Each instance can be monitored independently:

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'ciyex-practice1'
    static_configs:
      - targets: ['ciyex-practice1:8080']
        labels:
          practice: 'practice1'
          
  - job_name: 'ciyex-practice2'
    static_configs:
      - targets: ['ciyex-practice2:8080']
        labels:
          practice: 'practice2'
```

## Troubleshooting

### Check Current Schema

```sql
-- Connect to database
psql -h localhost -U postgres -d ciyexdb

-- Check current schema
SELECT current_schema();

-- Check database default search path
SHOW search_path;

-- List all schemas
SELECT schema_name FROM information_schema.schemata 
WHERE schema_name NOT IN ('pg_catalog', 'information_schema');
```

### Verify Instance Configuration

Check the logs at startup:

```
INFO  SingleSchemaConfig - Initializing single schema configuration: practice_1
INFO  SingleSchemaConfig - Ensured schema exists: practice_1
INFO  SingleSchemaConfig - Successfully configured instance to use schema: practice_1
INFO  SingleSchemaConfig - All database operations will use this schema by default
```

### Common Issues

**Issue**: Tables not found
- **Cause**: Schema not created or migrations not run
- **Solution**: Ensure schema exists and run Flyway migrations

**Issue**: Wrong schema being used
- **Cause**: CIYEX_SCHEMA_NAME not set correctly
- **Solution**: Check environment variable and restart application

**Issue**: Permission denied
- **Cause**: Database user lacks schema permissions
- **Solution**: Grant appropriate permissions: `GRANT ALL ON SCHEMA practice_1 TO postgres;`

## Best Practices

1. **Use descriptive schema names**: `practice_acme_clinic` instead of `practice_1`
2. **Set resource limits** per instance in Kubernetes/Docker
3. **Monitor each instance** separately with distinct labels
4. **Use separate databases** for production vs staging practices
5. **Backup schemas independently** for better recovery options
6. **Document which instance serves which practice** in your infrastructure
7. **Use environment-specific configs** (dev, staging, prod) per practice

## Security Considerations

1. **Network Isolation**: Use separate networks/VPCs per practice if needed
2. **Database Permissions**: Grant only necessary schema permissions
3. **API Authentication**: Each instance can have separate Keycloak realms/clients
4. **Audit Logging**: Logs are naturally separated per instance
5. **Data Encryption**: Configure per-instance encryption keys if needed

## Future Enhancements

If you need to support multiple practices in the future:

1. **Option 1**: Deploy more instances (recommended)
2. **Option 2**: Re-enable multi-tenant components (not recommended)
3. **Option 3**: Use separate databases per practice (maximum isolation)

The single-schema-per-instance approach scales well and is the recommended architecture for healthcare applications requiring strong data isolation.
