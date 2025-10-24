# Tenant Access Control with Keycloak Groups

## Overview

This system implements hierarchical access control using Keycloak groups to manage user access to applications and tenants (organizations).

## Group Hierarchy

### 1. Apps Group (Application Level)
Users in the **Apps** group have access to ALL tenants within that application.

```
/Apps
  ├── /Ciyex    (Access to all Ciyex tenants)
  └── /Aran     (Access to all Aran tenants)
```

### 2. Tenants Group (Organization Level)
Users in the **Tenants** group have access to SPECIFIC tenants only.

```
/Tenants
  ├── /Qiaben Health  (Access to Qiaben Health only)
  ├── /MediPlus       (Access to MediPlus only)
  └── /CareWell       (Access to CareWell only)
```

## Access Control Logic

### Scenario 1: User in Apps Group
**Example:** User belongs to `/Apps/Ciyex`

- ✅ Has access to ALL tenants (Qiaben Health, MediPlus, CareWell)
- ✅ **MUST** provide `X-Org-Id` header to specify which tenant
- ✅ Can switch between tenants freely

**Response from `/api/auth/keycloak-callback`:**
```json
{
  "hasFullAccess": true,
  "accessibleTenants": ["ALL"],
  "requiresOrgId": true,
  "defaultTenant": null
}
```

### Scenario 2: User with Single Tenant
**Example:** User belongs to `/Tenants/Qiaben Health` only

- ✅ Has access to Qiaben Health tenant only
- ✅ **NO** `X-Org-Id` header required (auto-selected)
- ❌ Cannot access other tenants

**Response from `/api/auth/keycloak-callback`:**
```json
{
  "hasFullAccess": false,
  "accessibleTenants": ["Qiaben Health"],
  "requiresOrgId": false,
  "defaultTenant": "Qiaben Health"
}
```

### Scenario 3: User with Multiple Tenants
**Example:** User belongs to `/Tenants/Qiaben Health` and `/Tenants/MediPlus`

- ✅ Has access to Qiaben Health and MediPlus only
- ✅ **MUST** provide `X-Org-Id` header to specify which tenant
- ❌ Cannot access CareWell

**Response from `/api/auth/keycloak-callback`:**
```json
{
  "hasFullAccess": false,
  "accessibleTenants": ["Qiaben Health", "MediPlus"],
  "requiresOrgId": true,
  "defaultTenant": null
}
```

## Implementation

### Backend Components

#### 1. TenantAccessService
**Location:** `src/main/java/com/qiaben/ciyex/service/TenantAccessService.java`

**Key Methods:**
- `hasAccessToAllTenants(groups)` - Check if user has Apps group access
- `getAccessibleTenants(groups)` - Get list of accessible tenant names
- `requiresOrgIdHeader(groups)` - Check if X-Org-Id header is required
- `getDefaultTenant(groups)` - Get default tenant for single-tenant users
- `resolveOrgId(groups, requestedOrgId)` - Resolve org ID for current request

#### 2. TenantResolutionFilter
**Location:** `src/main/java/com/qiaben/ciyex/filter/TenantResolutionFilter.java`

**Functionality:**
- Runs after JWT authentication filter
- Extracts groups from JWT token
- Resolves org ID based on groups and X-Org-Id header
- Sets org ID in `RequestContext` for downstream use
- Returns 400 error if X-Org-Id is required but not provided

#### 3. AuthController Enhancement
**Location:** `src/main/java/com/qiaben/ciyex/controller/AuthController.java`

**Keycloak Callback Response:**
```json
{
  "success": true,
  "message": "Authentication successful",
  "data": {
    "token": "eyJhbGc...",
    "email": "user@example.com",
    "username": "user",
    "firstName": "John",
    "lastName": "Doe",
    "userId": "123",
    "groups": ["/Apps/Ciyex", "/Tenants/Qiaben Health"],
    "hasFullAccess": true,
    "accessibleTenants": ["ALL"],
    "requiresOrgId": true,
    "defaultTenant": null
  }
}
```

## Frontend Integration

### 1. Store Tenant Access Info After Login

```typescript
// In callback page after successful authentication
const { 
  hasFullAccess, 
  accessibleTenants, 
  requiresOrgId, 
  defaultTenant 
} = data.data;

localStorage.setItem("hasFullAccess", hasFullAccess);
localStorage.setItem("accessibleTenants", JSON.stringify(accessibleTenants));
localStorage.setItem("requiresOrgId", requiresOrgId);
if (defaultTenant) {
  localStorage.setItem("defaultTenant", defaultTenant);
}
```

### 2. Add X-Org-Id Header When Required

```typescript
// In API utility function
const makeApiRequest = async (url: string, options: RequestInit = {}) => {
  const requiresOrgId = localStorage.getItem("requiresOrgId") === "true";
  const selectedOrgId = localStorage.getItem("selectedOrgId");
  
  const headers = {
    ...options.headers,
    "Authorization": `Bearer ${localStorage.getItem("token")}`,
  };
  
  // Add X-Org-Id header if required
  if (requiresOrgId && selectedOrgId) {
    headers["X-Org-Id"] = selectedOrgId;
  }
  
  return fetch(url, { ...options, headers });
};
```

### 3. Tenant Selector Component (for multi-tenant users)

```typescript
const TenantSelector = () => {
  const requiresOrgId = localStorage.getItem("requiresOrgId") === "true";
  const accessibleTenants = JSON.parse(
    localStorage.getItem("accessibleTenants") || "[]"
  );
  
  if (!requiresOrgId) {
    return null; // No selector needed for single-tenant users
  }
  
  return (
    <select onChange={(e) => localStorage.setItem("selectedOrgId", e.target.value)}>
      <option value="">Select Tenant</option>
      {accessibleTenants.map(tenant => (
        <option key={tenant} value={getTenantOrgId(tenant)}>
          {tenant}
        </option>
      ))}
    </select>
  );
};
```

## API Request Flow

### Request with X-Org-Id Header

```bash
curl http://localhost:8080/api/patients/count \
  -H "Authorization: Bearer eyJhbGc..." \
  -H "X-Org-Id: 1"
```

**Flow:**
1. JWT filter validates token
2. TenantResolutionFilter extracts groups from token
3. Checks if user has access to org ID 1
4. Sets org ID in RequestContext
5. Request proceeds to controller
6. Controller uses org ID from RequestContext

### Request without X-Org-Id Header (Single Tenant User)

```bash
curl http://localhost:8080/api/patients/count \
  -H "Authorization: Bearer eyJhbGc..."
```

**Flow:**
1. JWT filter validates token
2. TenantResolutionFilter extracts groups from token
3. Detects user has only one tenant
4. Auto-resolves org ID from user's single tenant
5. Sets org ID in RequestContext
6. Request proceeds normally

### Request without X-Org-Id Header (Multi-Tenant User)

```bash
curl http://localhost:8080/api/patients/count \
  -H "Authorization: Bearer eyJhbGc..."
```

**Response:**
```json
{
  "error": "X-Org-Id header is required",
  "message": "You have access to multiple tenants. Please specify X-Org-Id header."
}
```
**Status:** 400 Bad Request

## Tenant to Org ID Mapping

Currently hardcoded in `TenantAccessService.getTenantOrgId()`:

```java
"qiaben health" -> 1L
"mediplus"      -> 2L
"carewell"      -> 3L
```

**TODO:** Replace with database lookup from `orgs` table.

## Database Schema

### Orgs Table
```sql
CREATE TABLE orgs (
    id BIGSERIAL PRIMARY KEY,
    org_name VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(50),
    country VARCHAR(100),
    postal_code VARCHAR(20),
    status VARCHAR(50)
);
```

**Sample Data:**
```sql
INSERT INTO orgs (org_name, city, state, country, status) VALUES
  ('Qiaben Health', 'Denver', 'CO', 'USA', 'ACTIVE'),
  ('MediPlus', 'Austin', 'TX', 'USA', 'ACTIVE'),
  ('CareWell', 'Seattle', 'WA', 'USA', 'ACTIVE');
```

## Keycloak Configuration

### 1. Create Groups

In Keycloak Admin Console:

**Apps Groups:**
1. Create group: `Apps`
2. Create subgroup: `Apps/Ciyex`
3. Create subgroup: `Apps/Aran`

**Tenants Groups:**
1. Create group: `Tenants`
2. Create subgroup: `Tenants/Qiaben Health`
3. Create subgroup: `Tenants/MediPlus`
4. Create subgroup: `Tenants/CareWell`

### 2. Assign Users to Groups

**Admin User (Full Access):**
- Add to: `/Apps/Ciyex`

**Single Tenant User:**
- Add to: `/Tenants/Qiaben Health`

**Multi-Tenant User:**
- Add to: `/Tenants/Qiaben Health`
- Add to: `/Tenants/MediPlus`

### 3. Configure Client Mappers

Ensure groups are included in JWT token:

1. Go to: Clients → `ciyex-app` → Client Scopes → `ciyex-app-dedicated`
2. Add mapper: **Group Membership**
   - Name: `groups`
   - Token Claim Name: `groups`
   - Full group path: ON
   - Add to ID token: ON
   - Add to access token: ON
   - Add to userinfo: ON

## Testing

### Test 1: Admin User (Full Access)

**Login as:** User with `/Apps/Ciyex` group

**Expected Response:**
```json
{
  "hasFullAccess": true,
  "requiresOrgId": true
}
```

**Test API Call:**
```bash
# Should work with X-Org-Id
curl http://localhost:8080/api/patients/count \
  -H "Authorization: Bearer TOKEN" \
  -H "X-Org-Id: 1"

# Should fail without X-Org-Id
curl http://localhost:8080/api/patients/count \
  -H "Authorization: Bearer TOKEN"
# Returns: 400 "X-Org-Id header is required"
```

### Test 2: Single Tenant User

**Login as:** User with `/Tenants/Qiaben Health` only

**Expected Response:**
```json
{
  "hasFullAccess": false,
  "requiresOrgId": false,
  "defaultTenant": "Qiaben Health"
}
```

**Test API Call:**
```bash
# Should work without X-Org-Id (auto-selected)
curl http://localhost:8080/api/patients/count \
  -H "Authorization: Bearer TOKEN"

# Should also work with X-Org-Id
curl http://localhost:8080/api/patients/count \
  -H "Authorization: Bearer TOKEN" \
  -H "X-Org-Id: 1"
```

### Test 3: Multi-Tenant User

**Login as:** User with `/Tenants/Qiaben Health` and `/Tenants/MediPlus`

**Expected Response:**
```json
{
  "hasFullAccess": false,
  "requiresOrgId": true,
  "accessibleTenants": ["Qiaben Health", "MediPlus"]
}
```

**Test API Call:**
```bash
# Should work with X-Org-Id: 1 (Qiaben Health)
curl http://localhost:8080/api/patients/count \
  -H "Authorization: Bearer TOKEN" \
  -H "X-Org-Id: 1"

# Should work with X-Org-Id: 2 (MediPlus)
curl http://localhost:8080/api/patients/count \
  -H "Authorization: Bearer TOKEN" \
  -H "X-Org-Id: 2"

# Should fail without X-Org-Id
curl http://localhost:8080/api/patients/count \
  -H "Authorization: Bearer TOKEN"
# Returns: 400 "X-Org-Id header is required"
```

## Security Considerations

1. **Group Validation:** Always validate that requested org ID matches user's accessible tenants
2. **Token Validation:** JWT tokens are validated by Spring Security before reaching tenant filter
3. **Schema Isolation:** Each tenant has separate PostgreSQL schema for data isolation
4. **Audit Logging:** All tenant access should be logged for compliance

## Future Enhancements

1. **Dynamic Tenant Mapping:** Query database for tenant-to-org-id mapping
2. **Tenant Switching UI:** Add dropdown in frontend for multi-tenant users
3. **Tenant Preferences:** Remember last selected tenant per user
4. **Tenant Permissions:** Fine-grained permissions within each tenant
5. **Cross-Tenant Queries:** Allow admins to query across multiple tenants

---

**Last Updated:** October 23, 2025
**Status:** ✅ Implemented and Ready for Testing
