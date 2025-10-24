# KeycloakUserService - Using Client Credentials

## Summary

Updated `KeycloakUserService` to use **client credentials grant** with the `ciyex-app` client instead of admin username/password. This is the proper way to authenticate service-to-service calls in Keycloak.

## Changes Made

### ✅ Updated Authentication Method

**Before (Admin Username/Password):**
```java
@Value("${keycloak.admin.username:aran-admin}")
private String adminUsername;

@Value("${keycloak.admin.password:}")
private String adminPassword;

// Token request
String tokenUrl = keycloakUrl + "/realms/master/protocol/openid-connect/token";
String body = "grant_type=password&client_id=admin-cli&username=" + adminUsername + "&password=" + adminPassword;
```

**After (Client Credentials):**
```java
@Value("${keycloak.resource}")
private String clientId;  // ciyex-app

@Value("${keycloak.credentials.secret}")
private String clientSecret;

// Token request
String tokenUrl = keycloakUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/token";
String body = "grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret;
```

## Why Client Credentials?

### ❌ Problems with Admin Username/Password:
1. **Security Risk** - Storing admin credentials in application config
2. **Wrong Realm** - Uses master realm instead of application realm
3. **Not Scalable** - Requires admin user for every service
4. **Audit Issues** - All actions appear as admin user
5. **Best Practice Violation** - Service accounts should use client credentials

### ✅ Benefits of Client Credentials:
1. **Secure** - Client secret can be rotated without changing code
2. **Proper Realm** - Uses application realm (ciyex)
3. **Service Account** - Dedicated service account for the application
4. **Auditable** - Actions tracked to specific client
5. **Best Practice** - OAuth 2.0 standard for service-to-service auth

## Keycloak Configuration Required

### 1. Enable Service Account for ciyex-app Client

In Keycloak Admin Console:

**a. Go to Client Settings:**
```
Clients → ciyex-app → Settings
```

**b. Enable Service Account:**
```
✅ Service Accounts Enabled
✅ Authorization Enabled (optional)
```

**c. Save**

### 2. Assign Required Roles to Service Account

The service account needs admin roles to manage users:

**a. Go to Service Account Roles:**
```
Clients → ciyex-app → Service Account Roles
```

**b. Assign Realm Management Roles:**

Click "Client Roles" → Select "realm-management" → Assign:
- ✅ `manage-users` - Create, update, delete users
- ✅ `view-users` - View user details
- ✅ `manage-realm` - Manage realm settings (optional)
- ✅ `view-realm` - View realm settings

**c. Assign Realm Roles (if needed):**

Click "Realm Roles" → Assign:
- ✅ `admin` - Full admin access (if needed)

### 3. Verify Client Secret

**a. Get Client Secret:**
```
Clients → ciyex-app → Credentials
```

**b. Copy the Secret:**
```
Secret: abc123def456...
```

**c. Update application.yml:**
```yaml
keycloak:
  auth-server-url: https://keycloak.example.com
  realm: ciyex
  resource: ciyex-app
  credentials:
    secret: abc123def456...  # From Keycloak
```

## Testing

### 1. Test Token Acquisition

```bash
curl -X POST "https://keycloak.example.com/realms/ciyex/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=ciyex-app" \
  -d "client_secret=abc123def456..."
```

**Expected Response:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 300,
  "token_type": "Bearer",
  "not-before-policy": 0,
  "scope": "profile email"
}
```

### 2. Test User Creation

```java
@Autowired
private KeycloakUserService keycloakUserService;

// Create user
String userId = keycloakUserService.createUser(
    "test@example.com",
    "Test",
    "User",
    "password123",
    Map.of("orgId", "1")
);

// Add to group
keycloakUserService.addUserToGroup(userId, "/Tenants/practice_1");

// Assign roles
keycloakUserService.assignRolesToUser(userId, List.of("patient"));
```

### 3. Verify in Keycloak

Check that:
- ✅ User was created
- ✅ User is in correct group
- ✅ User has assigned roles

## Token Details

### Access Token Claims:

```json
{
  "exp": 1730000000,
  "iat": 1729999700,
  "jti": "abc-123-def",
  "iss": "https://keycloak.example.com/realms/ciyex",
  "aud": "account",
  "sub": "service-account-ciyex-app",
  "typ": "Bearer",
  "azp": "ciyex-app",
  "realm_access": {
    "roles": ["manage-users", "view-users"]
  },
  "resource_access": {
    "realm-management": {
      "roles": ["manage-users", "view-users"]
    }
  },
  "scope": "profile email",
  "clientId": "ciyex-app",
  "clientHost": "10.0.0.1",
  "preferred_username": "service-account-ciyex-app"
}
```

## Security Best Practices

### 1. Rotate Client Secret Regularly

```bash
# In Keycloak Admin Console
Clients → ciyex-app → Credentials → Regenerate Secret
```

### 2. Use Environment Variables

```yaml
keycloak:
  credentials:
    secret: ${KEYCLOAK_CLIENT_SECRET}  # From environment
```

### 3. Limit Permissions

Only assign roles that are actually needed:
- ✅ `manage-users` - For user CRUD
- ✅ `view-users` - For user queries
- ❌ `manage-realm` - Usually not needed
- ❌ `admin` - Too broad

### 4. Monitor Service Account Usage

Check Keycloak logs for service account activity:
```
Events → Login Events → Filter by "service-account-ciyex-app"
```

## Troubleshooting

### Error: "Client not found"

**Solution:** Verify client ID in application.yml matches Keycloak:
```yaml
keycloak:
  resource: ciyex-app  # Must match client ID in Keycloak
```

### Error: "Invalid client credentials"

**Solution:** Regenerate and update client secret:
```bash
# Get new secret from Keycloak
Clients → ciyex-app → Credentials → Regenerate

# Update application.yml
keycloak:
  credentials:
    secret: <new-secret>
```

### Error: "Insufficient permissions"

**Solution:** Assign required roles to service account:
```
Clients → ciyex-app → Service Account Roles → 
  Client Roles → realm-management → 
    Assign: manage-users, view-users
```

### Error: "Service accounts not enabled"

**Solution:** Enable service accounts for client:
```
Clients → ciyex-app → Settings → 
  ✅ Service Accounts Enabled → Save
```

## Application Configuration

### Complete application.yml:

```yaml
keycloak:
  auth-server-url: ${KEYCLOAK_URL:https://keycloak.example.com}
  realm: ${KEYCLOAK_REALM:ciyex}
  resource: ${KEYCLOAK_CLIENT_ID:ciyex-app}
  credentials:
    secret: ${KEYCLOAK_CLIENT_SECRET}
  ssl-required: external
  public-client: false
  confidential-port: 0
  use-resource-role-mappings: true
```

### Environment Variables:

```bash
export KEYCLOAK_URL=https://keycloak.example.com
export KEYCLOAK_REALM=ciyex
export KEYCLOAK_CLIENT_ID=ciyex-app
export KEYCLOAK_CLIENT_SECRET=abc123def456...
```

## Migration Steps

### For Existing Deployments:

1. **Enable Service Account in Keycloak**
   - Go to ciyex-app client settings
   - Enable "Service Accounts Enabled"
   - Save

2. **Assign Required Roles**
   - Go to Service Account Roles tab
   - Assign realm-management roles
   - Save

3. **Update Application Config**
   - Remove admin username/password
   - Add client secret
   - Restart application

4. **Test User Management**
   - Try creating a test user
   - Verify it works
   - Delete test user

5. **Remove Admin Credentials**
   - Remove from config files
   - Remove from environment variables
   - Update secrets management

## Benefits Summary

### ✅ Security
- No admin credentials in application
- Client secret can be rotated
- Proper OAuth 2.0 flow

### ✅ Scalability
- Each service can have its own client
- No shared admin account
- Better resource isolation

### ✅ Auditability
- Actions tracked to specific client
- Better logging and monitoring
- Compliance friendly

### ✅ Maintainability
- Standard OAuth 2.0 pattern
- Easier to understand
- Better documentation

---

**Status**: Updated to use client credentials ✅  
**Authentication**: OAuth 2.0 Client Credentials Grant  
**Next**: Configure Keycloak service account and test
