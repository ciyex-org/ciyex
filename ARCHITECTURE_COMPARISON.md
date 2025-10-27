# Architecture Comparison: Multi-Tenant vs Single-Schema

## Side-by-Side Comparison

| Aspect | Multi-Tenant (Old) | Single-Schema (New) |
|--------|-------------------|---------------------|
| **Deployment Model** | One instance for all practices | One instance per practice |
| **Schema Selection** | Dynamic at runtime | Static at startup |
| **API Headers** | Requires `X-Tenant-Name` | No tenant headers |
| **Configuration** | Complex tenant resolution | Simple env variable |
| **Data Isolation** | Schema-level (same process) | Process-level (separate instances) |
| **Scaling** | Vertical (bigger instance) | Horizontal (more instances) |
| **Resource Sharing** | Shared connection pools | Dedicated resources |
| **Troubleshooting** | Complex (mixed logs) | Simple (isolated logs) |
| **Security Risk** | Medium (header-based routing) | Low (process isolation) |
| **Operational Complexity** | High | Low |

## Component Comparison

### Request Flow

#### Multi-Tenant (Old)
```
HTTP Request
    ↓
[X-Tenant-Name: Practice1]
    ↓
TenantResolutionFilter
    ↓
Validate tenant access (Keycloak)
    ↓
TenantContextInterceptor
    ↓
Set RequestContext.tenantName
    ↓
TenantIdentifierResolver
    ↓
Resolve schema name
    ↓
TenantSchemaInterceptor
    ↓
SET search_path TO practice_1
    ↓
Execute query in practice_1 schema
    ↓
Clear RequestContext
    ↓
Response
```

#### Single-Schema (New)
```
HTTP Request
    ↓
[No tenant header needed]
    ↓
Execute query in configured schema
    ↓
Response
```

### Configuration Comparison

#### Multi-Tenant (Old)
```yaml
# application.yml
ciyex:
  tenant:
    auto-init: false
    auto-create-on-request: true

keycloak:
  enabled: true
  auth-server-url: https://keycloak.example.com
  realm: master

# Active Components:
# - TenantResolutionFilter
# - TenantContextInterceptor
# - TenantSchemaInterceptor
# - TenantIdentifierResolver
# - MultiTenantConnectionProvider
```

#### Single-Schema (New)
```yaml
# application.yml
ciyex:
  schema:
    name: ${CIYEX_SCHEMA_NAME:public}

# Active Components:
# - SingleSchemaConfig

# Disabled Components:
# - TenantResolutionFilter (commented out)
# - TenantContextInterceptor (commented out)
# - TenantSchemaInterceptor (commented out)
# - TenantIdentifierResolver (commented out)
# - MultiTenantConnectionProvider (commented out)
```

### API Call Comparison

#### Multi-Tenant (Old)
```bash
# Single endpoint for all practices
curl -X GET https://api.example.com/api/patients \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Name: Practice1"

# Different tenant, same endpoint
curl -X GET https://api.example.com/api/patients \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Name: Practice2"
```

#### Single-Schema (New)
```bash
# Practice 1 instance
curl -X GET https://practice1.example.com/api/patients \
  -H "Authorization: Bearer $TOKEN"

# Practice 2 instance (different URL)
curl -X GET https://practice2.example.com/api/patients \
  -H "Authorization: Bearer $TOKEN"
```

### Deployment Comparison

#### Multi-Tenant (Old)
```bash
# Single deployment
docker run -p 8080:8080 \
  -e KEYCLOAK_ENABLED=true \
  -e CIYEX_TENANT_AUTO_CREATE=true \
  ciyex:latest

# Serves all practices from one instance
```

#### Single-Schema (New)
```bash
# Practice 1 deployment
docker run -p 8081:8080 \
  -e CIYEX_SCHEMA_NAME=practice_1 \
  ciyex:latest

# Practice 2 deployment
docker run -p 8082:8080 \
  -e CIYEX_SCHEMA_NAME=practice_2 \
  ciyex:latest

# Each practice has dedicated instance
```

## Database Architecture

### Multi-Tenant (Old)
```
┌─────────────────────────────────────────┐
│           PostgreSQL Database           │
│                                         │
│  ┌────────────────────────────────┐    │
│  │      public (master schema)    │    │
│  │  - users                       │    │
│  │  - orgs                        │    │
│  │  - org_config                  │    │
│  └────────────────────────────────┘    │
│                                         │
│  ┌────────────────────────────────┐    │
│  │      practice_1 schema         │    │
│  │  - patients                    │    │
│  │  - appointments                │    │
│  │  - encounters                  │    │
│  └────────────────────────────────┘    │
│                                         │
│  ┌────────────────────────────────┐    │
│  │      practice_2 schema         │    │
│  │  - patients                    │    │
│  │  - appointments                │    │
│  │  - encounters                  │    │
│  └────────────────────────────────┘    │
│                                         │
└─────────────────────────────────────────┘
         ↑
         │ (Dynamic schema switching)
         │
┌────────┴────────┐
│  Single App     │
│  Instance       │
└─────────────────┘
```

### Single-Schema (New)
```
┌─────────────────────────────────────────┐
│           PostgreSQL Database           │
│                                         │
│  ┌────────────────────────────────┐    │
│  │      public (shared schema)    │    │
│  │  - users (if needed)           │    │
│  └────────────────────────────────┘    │
│                                         │
│  ┌────────────────────────────────┐    │
│  │      practice_1 schema         │◄───┼─┐
│  │  - patients                    │    │ │
│  │  - appointments                │    │ │
│  │  - encounters                  │    │ │
│  └────────────────────────────────┘    │ │
│                                         │ │
│  ┌────────────────────────────────┐    │ │
│  │      practice_2 schema         │◄───┼─┼─┐
│  │  - patients                    │    │ │ │
│  │  - appointments                │    │ │ │
│  │  - encounters                  │    │ │ │
│  └────────────────────────────────┘    │ │ │
│                                         │ │ │
└─────────────────────────────────────────┘ │ │
                                            │ │
┌───────────────────┐  ┌──────────────────┐│ │
│  App Instance 1   │  │  App Instance 2  ││ │
│  (Practice 1)     │  │  (Practice 2)    ││ │
│  Port: 8081       │  │  Port: 8082      ││ │
└───────────────────┘  └──────────────────┘│ │
         │                      │            │ │
         └──────────────────────┼────────────┘ │
                                └──────────────┘
```

## Security Comparison

| Security Aspect | Multi-Tenant (Old) | Single-Schema (New) |
|----------------|-------------------|---------------------|
| **Process Isolation** | ❌ Shared process | ✅ Separate processes |
| **Memory Isolation** | ❌ Shared memory | ✅ Separate memory |
| **Connection Pools** | ❌ Shared pools | ✅ Dedicated pools |
| **Attack Surface** | ❌ Header manipulation risk | ✅ No header-based routing |
| **Data Leakage Risk** | ⚠️ Medium (same process) | ✅ Low (process isolation) |
| **Audit Trail** | ⚠️ Mixed logs | ✅ Separate logs |
| **Compliance** | ⚠️ More complex | ✅ Simpler to prove |

## Performance Comparison

| Metric | Multi-Tenant (Old) | Single-Schema (New) |
|--------|-------------------|---------------------|
| **Request Overhead** | High (tenant resolution) | Low (no resolution) |
| **Schema Switching** | Every request | None (set at startup) |
| **Connection Pool** | Shared (contention) | Dedicated (no contention) |
| **Memory Usage** | Higher (tenant context) | Lower (simpler) |
| **CPU Usage** | Higher (more logic) | Lower (less logic) |
| **Scalability** | Vertical only | Horizontal (per practice) |

### Benchmark Example

```
Multi-Tenant (Old):
- Tenant resolution: ~5ms
- Schema switching: ~2ms
- Query execution: 10ms
- Total: ~17ms per request

Single-Schema (New):
- Tenant resolution: 0ms (none)
- Schema switching: 0ms (none)
- Query execution: 10ms
- Total: ~10ms per request

Performance improvement: ~40% faster
```

## Operational Comparison

### Monitoring

#### Multi-Tenant (Old)
```
# Single instance to monitor
# Must filter logs by tenant
# Shared metrics across all tenants
# Complex to identify tenant-specific issues

Metrics:
- Total requests (all tenants)
- Total memory (shared)
- Total CPU (shared)
- Connection pool (shared)
```

#### Single-Schema (New)
```
# Multiple instances to monitor
# Logs naturally separated
# Dedicated metrics per practice
# Easy to identify practice-specific issues

Metrics per instance:
- Requests for practice_1
- Memory for practice_1
- CPU for practice_1
- Connection pool for practice_1
```

### Troubleshooting

#### Multi-Tenant (Old)
```bash
# Find logs for specific tenant
grep "tenantName=Practice1" application.log

# Check which schema was used
grep "practice_1" application.log

# Identify tenant-specific errors
grep "ERROR.*Practice1" application.log

# Complex: logs mixed with other tenants
```

#### Single-Schema (New)
```bash
# All logs are for one practice
tail -f practice1.log

# No filtering needed
grep "ERROR" practice1.log

# Simple: logs are naturally isolated
```

### Deployment

#### Multi-Tenant (Old)
```yaml
# Single deployment affects all practices
# Risky: one bug affects everyone
# Rolling updates complex
# Rollback affects all tenants

deployment:
  replicas: 3
  # All replicas serve all practices
```

#### Single-Schema (New)
```yaml
# Independent deployments per practice
# Safe: issues isolated to one practice
# Rolling updates simple
# Rollback only affects one practice

practice1-deployment:
  replicas: 2
  
practice2-deployment:
  replicas: 3
  # Can scale independently
```

## Cost Comparison

### Infrastructure Costs

| Cost Factor | Multi-Tenant (Old) | Single-Schema (New) |
|------------|-------------------|---------------------|
| **Compute** | 1 large instance | Multiple small instances |
| **Memory** | High (shared overhead) | Lower per instance |
| **Network** | Single endpoint | Multiple endpoints |
| **Load Balancer** | Simple routing | Domain/path routing |
| **Monitoring** | Single dashboard | Multiple dashboards |
| **Total Cost** | Lower upfront | Higher upfront |

### Operational Costs

| Cost Factor | Multi-Tenant (Old) | Single-Schema (New) |
|------------|-------------------|---------------------|
| **Development** | Complex (tenant logic) | Simple (no tenant logic) |
| **Testing** | Complex (test isolation) | Simple (test one schema) |
| **Debugging** | Time-consuming | Faster |
| **Maintenance** | Higher (complex code) | Lower (simpler code) |
| **Training** | More time needed | Less time needed |
| **Total Cost** | Higher long-term | Lower long-term |

## When to Use Each

### Use Multi-Tenant When:
- ❌ You have hundreds of small practices (not recommended for healthcare)
- ❌ Practices are okay with shared resources
- ❌ Cost is the primary concern
- ❌ You have strong tenant isolation guarantees

### Use Single-Schema When:
- ✅ You have healthcare data (HIPAA compliance)
- ✅ You need strong data isolation
- ✅ You want independent scaling per practice
- ✅ You want simpler operations
- ✅ You have < 100 practices
- ✅ **This is the recommended approach**

## Migration Effort

| Task | Effort | Risk |
|------|--------|------|
| Code changes | Low (mostly disabling) | Low |
| Configuration changes | Low | Low |
| Infrastructure setup | Medium | Medium |
| Client updates | Medium | Medium |
| Testing | Medium | Low |
| Deployment | High | Medium |
| **Total** | **Medium** | **Medium** |

## Conclusion

The single-schema-per-instance architecture is **recommended** for healthcare applications because:

1. ✅ **Better Security** - Complete process isolation
2. ✅ **Simpler Code** - No complex tenant logic
3. ✅ **Easier Operations** - Independent deployments
4. ✅ **Better Performance** - No tenant resolution overhead
5. ✅ **Compliance** - Easier to audit and prove isolation

While it requires more infrastructure (multiple instances), the operational benefits and security improvements make it the right choice for healthcare data.

---

**Recommendation**: Use single-schema-per-instance for all new deployments and migrate existing multi-tenant deployments when possible.
