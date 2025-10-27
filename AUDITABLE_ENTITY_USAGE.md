# Auditable Entity Usage Guide

## Overview

The `AuditableEntity` base class provides automatic tracking of audit information for all entities that extend it. This includes:

- **Created Date**: Timestamp when the entity was first persisted
- **Last Modified Date**: Timestamp when the entity was last updated
- **Created By**: User who created the entity
- **Last Modified By**: User who last modified the entity
- **Tenant Name**: Automatically populated from `RequestContext.getTenantName()`

## How to Use

### 1. Extend AuditableEntity

Simply extend `AuditableEntity` in your entity class:

```java
@Entity
@Table(name = "products")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Product extends AuditableEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    private String description;
    
    private Double price;
}
```

### 2. Database Schema

Ensure your database table includes the auditable columns:

```sql
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2),
    
    -- Auditable fields (automatically managed)
    created_date TIMESTAMP NOT NULL,
    last_modified_date TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    last_modified_by VARCHAR(100),
    tenant_name VARCHAR(100) NOT NULL
);
```

### 3. RequestContext Setup

The `tenantName` field is automatically populated from `RequestContext`. Ensure your interceptor or filter sets up the RequestContext:

```java
@Component
public class TenantInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                           HttpServletResponse response, 
                           Object handler) {
        RequestContext context = new RequestContext();
        
        // Extract tenant from header, JWT, or other source
        String tenantName = request.getHeader("X-Tenant-Name");
        context.setTenantName(tenantName);
        
        // Set auth token for user tracking
        String authToken = request.getHeader("Authorization");
        context.setAuthToken(authToken);
        
        RequestContext.set(context);
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, 
                               HttpServletResponse response, 
                               Object handler, 
                               Exception ex) {
        RequestContext.clear();
    }
}
```

## Features

### Automatic Timestamp Management

- `createdDate` is set automatically when the entity is first persisted
- `lastModifiedDate` is updated automatically on every update

### User Tracking

- `createdBy` and `lastModifiedBy` are populated from the `AuditorAware` implementation
- Currently uses information from `RequestContext` (can be customized in `JpaAuditingConfig`)

### Tenant Isolation

- `tenantName` is automatically populated from `RequestContext.getTenantName()`
- The field is immutable (cannot be changed after creation)
- `TenantAuditListener` validates tenant consistency on updates

### Security

The `TenantAuditListener` includes security validation:

```java
@PreUpdate
public void validateTenantOnUpdate(Object entity) {
    // Prevents updating entities from a different tenant
    // Throws IllegalStateException if tenant mismatch detected
}
```

## Customization

### Custom User Extraction

To customize how the current user is extracted, modify the `extractUserFromContext` method in `JpaAuditingConfig`:

```java
private String extractUserFromContext(RequestContext context) {
    String authToken = context.getAuthToken();
    if (authToken != null && !authToken.isEmpty()) {
        // Parse JWT token to extract username
        String username = jwtTokenUtil.getUsernameFromToken(authToken);
        return username;
    }
    return "system";
}
```

### Adding More Audit Fields

You can extend `AuditableEntity` to add more audit fields:

```java
@MappedSuperclass
public abstract class ExtendedAuditableEntity extends AuditableEntity {
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    // Add custom @PrePersist or @PreUpdate methods as needed
}
```

## Migration Guide

To migrate existing entities to use `AuditableEntity`:

1. **Add the auditable columns** to your database table
2. **Extend AuditableEntity** in your entity class
3. **Remove manual audit field management** from your code
4. **Update existing records** (optional):

```sql
UPDATE your_table 
SET created_date = CURRENT_TIMESTAMP,
    last_modified_date = CURRENT_TIMESTAMP,
    created_by = 'migration',
    last_modified_by = 'migration',
    tenant_name = 'default_tenant'
WHERE created_date IS NULL;
```

## Example: Complete Entity

```java
package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "appointments", schema = "practice_1")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentExample extends AuditableEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "patient_id", nullable = false)
    private Long patientId;
    
    @Column(name = "provider_id", nullable = false)
    private Long providerId;
    
    @Column(name = "appointment_date", nullable = false)
    private LocalDateTime appointmentDate;
    
    @Column(name = "status", nullable = false)
    private String status;
    
    @Column(name = "notes")
    private String notes;
    
    // No need to manually manage audit fields - they're inherited from AuditableEntity!
}
```

## Notes

- The `tenantName` field is **immutable** after creation for data integrity
- All audit fields are managed automatically - don't set them manually
- The `@PrePersist` and `@PreUpdate` callbacks in `AuditableEntity` ensure timestamps are always set
- If `RequestContext` is not available, the system will log warnings but won't fail
