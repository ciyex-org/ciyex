# Keycloak Group Attributes Setup Guide

## Overview

This guide shows how to store org and org_config data directly in Keycloak group attributes, eliminating the need for database-based tenant mappings and enabling data replication across multiple applications.

## Why Group Attributes?

✅ **Single Source of Truth** - All org data stored in Keycloak  
✅ **Multi-App Support** - Any application can read the same data  
✅ **No Database Coupling** - No need for tenant_mappings table  
✅ **Centralized Management** - Manage org data in one place  
✅ **Automatic Sync** - Changes in Keycloak immediately available to all apps  

## Step 1: Create Group Structure in Keycloak

### 1.1 Login to Keycloak Admin Console

Navigate to: `https://aran-stg.zpoa.com/admin/master/console`

### 1.2 Create Groups

**Create Apps Group:**
1. Go to: **Groups** → **Create group**
2. Name: `Apps`
3. Click **Create**

**Create Apps Subgroups:**
1. Select `Apps` group
2. Click **Create child group**
3. Name: `Ciyex`
4. Click **Create**
5. Repeat for `Aran`

**Create Tenants Group:**
1. Go to: **Groups** → **Create group**
2. Name: `Tenants`
3. Click **Create**

**Create Tenant Subgroups:**
1. Select `Tenants` group
2. Click **Create child group**
3. Name: `Qiaben Health`
4. Click **Create**
5. Repeat for `MediPlus`, `CareWell`

**Final Structure:**
```
/Apps
  ├── /Ciyex
  └── /Aran
/Tenants
  ├── /Qiaben Health
  ├── /MediPlus
  └── /CareWell
```

## Step 2: Add Attributes to Tenant Groups

### 2.1 Add Attributes to "Qiaben Health" Group

1. Go to: **Groups** → **Tenants** → **Qiaben Health**
2. Click **Attributes** tab
3. Add the following attributes:

| Key | Value |
|-----|-------|
| `org_id` | `1` |
| `org_name` | `Qiaben Health` |
| `address` | `123 Main St` |
| `city` | `Denver` |
| `state` | `CO` |
| `country` | `USA` |
| `postal_code` | `80202` |
| `status` | `ACTIVE` |

**Add any additional org_config fields:**
| Key | Value |
|-----|-------|
| `timezone` | `America/Denver` |
| `currency` | `USD` |
| `language` | `en` |
| `max_users` | `100` |
| `subscription_tier` | `premium` |

4. Click **Save**

### 2.2 Add Attributes to "MediPlus" Group

1. Go to: **Groups** → **Tenants** → **MediPlus**
2. Click **Attributes** tab
3. Add attributes:

| Key | Value |
|-----|-------|
| `org_id` | `2` |
| `org_name` | `MediPlus` |
| `address` | `456 Elm St` |
| `city` | `Austin` |
| `state` | `TX` |
| `country` | `USA` |
| `postal_code` | `73301` |
| `status` | `ACTIVE` |

4. Click **Save**

### 2.3 Add Attributes to "CareWell" Group

1. Go to: **Groups** → **Tenants** → **CareWell**
2. Click **Attributes** tab
3. Add attributes:

| Key | Value |
|-----|-------|
| `org_id` | `3` |
| `org_name` | `CareWell` |
| `address` | `789 Oak St` |
| `city` | `Seattle` |
| `state` | `WA` |
| `country` | `USA` |
| `postal_code` | `98101` |
| `status` | `ACTIVE` |

4. Click **Save**

## Step 3: Configure Client to Include Group Attributes in Token

### 3.1 Create Group Attributes Mapper

1. Go to: **Clients** → `ciyex-app`
2. Click **Client scopes** tab
3. Click on `ciyex-app-dedicated` scope
4. Click **Add mapper** → **By configuration** → **User Attribute**

**Configure Mapper:**
- **Name:** `group-attributes`
- **User Attribute:** Leave empty (we'll use a script)
- **Token Claim Name:** `group_attributes`
- **Claim JSON Type:** JSON
- **Add to ID token:** ON
- **Add to access token:** ON
- **Add to userinfo:** ON
- **Multivalued:** OFF
- **Aggregate attribute values:** OFF

5. Click **Save**

### 3.2 Alternative: Use Script Mapper (Recommended)

If the above doesn't work, use a **Script Mapper**:

1. Go to: **Clients** → `ciyex-app` → **Client scopes** → `ciyex-app-dedicated`
2. Click **Add mapper** → **By configuration** → **Script Mapper**

**Configure:**
- **Name:** `group-attributes-script`
- **Token Claim Name:** `group_attributes`
- **Claim JSON Type:** JSON
- **Add to ID token:** ON
- **Add to access token:** ON
- **Add to userinfo:** ON

**Script:**
```javascript
/**
 * Available variables:
 * user - the current user
 * realm - the current realm
 * token - the current token
 * userSession - the current userSession
 * keycloakSession - the current keycloakSession
 */

var groupAttributes = {};

// Get user's groups
var groups = user.getGroupsStream().toArray();

for (var i = 0; i < groups.length; i++) {
    var group = groups[i];
    var groupPath = group.getPathFriendly();
    var attrs = {};
    
    // Get all attributes from the group
    var groupAttrs = group.getAttributes();
    for (var key in groupAttrs) {
        if (groupAttrs.hasOwnProperty(key)) {
            var values = groupAttrs[key];
            // Take first value if multiple
            attrs[key] = values.length > 0 ? values[0] : null;
        }
    }
    
    // Only include groups with attributes
    if (Object.keys(attrs).length > 0) {
        groupAttributes[groupPath] = attrs;
    }
}

exports = groupAttributes;
```

3. Click **Save**

### 3.3 Verify Token Contains Group Attributes

1. Get a new access token by logging in
2. Decode the JWT token at: https://jwt.io
3. Verify the token contains:

```json
{
  "groups": [
    "/Tenants/Qiaben Health"
  ],
  "group_attributes": {
    "/Tenants/Qiaben Health": {
      "org_id": "1",
      "org_name": "Qiaben Health",
      "address": "123 Main St",
      "city": "Denver",
      "state": "CO",
      "country": "USA",
      "postal_code": "80202",
      "status": "ACTIVE"
    }
  }
}
```

## Step 4: Assign Users to Groups

### 4.1 Admin User (Full Access)

1. Go to: **Users** → Select user
2. Click **Groups** tab
3. Click **Join Group**
4. Select `/Apps/Ciyex`
5. Click **Join**

### 4.2 Single Tenant User

1. Go to: **Users** → Select user
2. Click **Groups** tab
3. Click **Join Group**
4. Select `/Tenants/Qiaben Health`
5. Click **Join**

### 4.3 Multi-Tenant User

1. Go to: **Users** → Select user
2. Click **Groups** tab
3. Click **Join Group**
4. Select `/Tenants/Qiaben Health`
5. Click **Join**
6. Click **Join Group** again
7. Select `/Tenants/MediPlus`
8. Click **Join**

## Step 5: Backend Implementation

The backend has been updated to:

1. **Extract group attributes from JWT token**
   - `KeycloakAuthService.extractGroupAttributesFromToken()`

2. **Resolve org ID from group attributes**
   - `TenantAccessService.getOrgIdFromGroupAttributes()`
   - `TenantAccessService.resolveOrgId()`

3. **Automatically set org ID in RequestContext**
   - `TenantResolutionFilter` extracts attributes and resolves org ID

## Step 6: Testing

### Test 1: Single Tenant User

**Login as:** User with `/Tenants/Qiaben Health` only

**Expected JWT Token:**
```json
{
  "groups": ["/Tenants/Qiaben Health"],
  "group_attributes": {
    "/Tenants/Qiaben Health": {
      "org_id": "1",
      "org_name": "Qiaben Health"
    }
  }
}
```

**API Call (No X-Org-Id needed):**
```bash
curl http://localhost:8080/api/patients/count \
  -H "Authorization: Bearer TOKEN"
```

**Expected:** Org ID 1 automatically resolved from group attributes

### Test 2: Multi-Tenant User

**Login as:** User with `/Tenants/Qiaben Health` and `/Tenants/MediPlus`

**Expected JWT Token:**
```json
{
  "groups": ["/Tenants/Qiaben Health", "/Tenants/MediPlus"],
  "group_attributes": {
    "/Tenants/Qiaben Health": {
      "org_id": "1"
    },
    "/Tenants/MediPlus": {
      "org_id": "2"
    }
  }
}
```

**API Call (X-Org-Id required):**
```bash
curl http://localhost:8080/api/patients/count \
  -H "Authorization: Bearer TOKEN" \
  -H "X-Org-Id: 1"
```

**Expected:** Org ID 1 validated against group attributes

### Test 3: Admin User

**Login as:** User with `/Apps/Ciyex`

**API Call (X-Org-Id required):**
```bash
curl http://localhost:8080/api/patients/count \
  -H "Authorization: Bearer TOKEN" \
  -H "X-Org-Id: 1"
```

**Expected:** Access to any org ID

## Step 7: Migrating Existing Data

### 7.1 Export Org Data from Database

```sql
SELECT 
    id as org_id,
    org_name,
    address,
    city,
    state,
    country,
    postal_code,
    status
FROM public.orgs;
```

### 7.2 Create Script to Sync to Keycloak

Create a script to read from database and update Keycloak groups via Admin API:

```bash
#!/bin/bash

# Keycloak Admin API credentials
KEYCLOAK_URL="https://aran-stg.zpoa.com"
REALM="master"
CLIENT_ID="admin-cli"
USERNAME="admin"
PASSWORD="your-password"

# Get admin token
TOKEN=$(curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -d "client_id=$CLIENT_ID" \
  -d "username=$USERNAME" \
  -d "password=$PASSWORD" \
  -d "grant_type=password" | jq -r '.access_token')

# Update group attributes
curl -X PUT "$KEYCLOAK_URL/admin/realms/$REALM/groups/{group-id}" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "attributes": {
      "org_id": ["1"],
      "org_name": ["Qiaben Health"],
      "address": ["123 Main St"],
      "city": ["Denver"],
      "state": ["CO"]
    }
  }'
```

## Benefits of This Approach

### 1. **Multi-Application Support**
- Ciyex EHR reads from Keycloak
- Aran Portal reads from Keycloak
- Any new app reads from Keycloak
- No database replication needed

### 2. **Centralized Management**
- Update org data once in Keycloak
- All applications see the change immediately
- No need to sync databases

### 3. **Simplified Architecture**
- No `tenant_mappings` table needed
- No database coupling between apps
- Keycloak is the single source of truth

### 4. **Better Security**
- Org data travels with the JWT token
- No additional database queries needed
- Reduced attack surface

### 5. **Easier Deployment**
- New applications don't need database access
- Just configure Keycloak client
- Read org data from token

## Troubleshooting

### Group Attributes Not in Token

**Problem:** Token doesn't contain `group_attributes` claim

**Solutions:**
1. Verify mapper is configured correctly
2. Check mapper is enabled
3. Ensure mapper is in the correct client scope
4. Try using Script Mapper instead of User Attribute mapper
5. Check Keycloak logs for errors

### Org ID Not Resolved

**Problem:** Backend can't resolve org ID

**Solutions:**
1. Verify group has `org_id` attribute
2. Check attribute value is numeric
3. Verify token contains group_attributes
4. Check backend logs for parsing errors

### Multiple Apps Not Syncing

**Problem:** Changes in Keycloak not reflected in other apps

**Solutions:**
1. Verify all apps use same Keycloak realm
2. Check all apps have correct client mappers
3. Ensure users re-login to get new tokens
4. Consider token refresh strategy

---

**Last Updated:** October 23, 2025  
**Status:** ✅ Ready for Implementation
