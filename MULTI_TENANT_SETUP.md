# Multi-Tenant Database Setup Guide

## Overview

This implementation provides a multi-tenant database architecture using schema-based isolation with the pattern `practice_{X-Org-Id}`. The system maintains user-organization relationships in the main `ciyexdb` database and routes data operations to tenant-specific schemas based on the `X-Org-Id` header.

## Architecture Components

### 1. Database Structure
- **Master Database**: `ciyexdb` - Contains user accounts, organizations, and user-org relationships
- **Tenant Schemas**: `practice_{orgId}` - Contains tenant-specific data (patients, appointments, etc.)

### 2. Key Components

#### Multi-Tenant Configuration
- `MultiTenantDataSourceConfig` - Configures routing datasource
- `TenantRoutingDataSource` - Routes connections based on organization context
- `TenantDataSourceProvider` - Creates and manages tenant-specific datasources
- `MultiTenantJpaConfig` - JPA configuration for multi-tenancy

#### Hibernate Integration
- `TenantIdentifierResolver` - Resolves current tenant from RequestContext
- `MultiTenantConnectionProvider` - Provides connections for specific tenants

#### Request Processing
- `TenantContextInterceptor` - Processes X-Org-Id header and validates access
- `RequestContext` - Thread-local storage for tenant information

#### Services
- `OrganizationAuthService` - Handles org-user relationships and validation
- `MultiTenantAuthService` - Manages tenant context initialization and switching

## Usage

### 1. Authentication Flow
1. User logs in with email/password
2. System looks up user's organizations in `ciyexdb`
3. Default organization is set in RequestContext
4. All subsequent operations use the tenant schema

### 2. API Requests
Include the `X-Org-Id` header in requests:
```
X-Org-Id: 123
```

This will route operations to the `practice_123` schema.

### 3. Schema Creation
Schemas are created automatically when first accessed. The system:
1. Creates the schema if it doesn't exist
2. Runs Hibernate DDL to create tables
3. Caches the datasource for future use

## Database Setup

### 1. Master Database Tables
Ensure these tables exist in `ciyexdb`:
- `users` - User accounts
- `orgs` - Organizations
- `user_org_roles` - User-organization relationships

### 2. Tenant Schema Tables
Each `practice_{orgId}` schema will contain:
- All entity tables (patients, appointments, encounters, etc.)
- Automatically created by Hibernate DDL

## Configuration

### application.yml
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate.multiTenancy: SCHEMA
      hibernate.hbm2ddl.create_namespaces: true
      hibernate.default_schema: public
```

## API Endpoints

### Tenant Management
- `GET /api/tenant/context` - Get current tenant context
- `GET /api/tenant/organizations` - Get user's organizations
- `POST /api/tenant/switch/{orgId}` - Switch to different organization
- `GET /api/tenant/test-isolation` - Test schema isolation

## Security Considerations

1. **Access Validation**: Users can only access organizations they belong to
2. **Schema Isolation**: Data is completely isolated between tenants
3. **Connection Pooling**: Each tenant has its own connection pool
4. **Automatic Cleanup**: RequestContext is cleared after each request

## Development Notes

### Adding New Entities
New entities will automatically be created in tenant schemas when:
1. The entity is annotated with JPA annotations
2. The package is included in `@EntityScan` or `packagesToScan`
3. Hibernate DDL is enabled

### Flyway Migrations
- Master (public schema) migrations live in `db/migration/master/<env>` with shared scripts in `db/migration/master/base`
- Tenant migrations are stored in `db/migration/tenant/<env>` with shared scripts in `db/migration/tenant/base`
- Control which environment-specific folder runs by setting the `CIYEX_ENV` environment variable (defaults to `local`)
- Flyway executes after the schema initializers finish building tables, so scripts should remain idempotent and focus on data or post-DDL adjustments

### Testing Multi-Tenancy
1. Create test organizations in `ciyexdb`
2. Assign users to organizations via `user_org_roles`
3. Use different `X-Org-Id` headers to test isolation
4. Verify data appears only in correct schemas

### Production Considerations
1. Set `hibernate.hbm2ddl.auto` to `validate` in production
2. Use database migrations for schema changes
3. Monitor connection pool usage per tenant
4. Consider schema cleanup for deleted organizations

## Troubleshooting

### Common Issues
1. **No schema found**: Ensure organization exists and user has access
2. **Connection errors**: Check database permissions for schema creation
3. **Data not isolated**: Verify RequestContext is properly set
4. **Performance issues**: Monitor connection pool sizes

### Debugging
Enable SQL logging to see which schema is being used:
```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
    com.qiaben.ciyex.multitenant: DEBUG
```
