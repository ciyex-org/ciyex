# User Entity References Removed

## Summary

Removed all remaining references to the deleted `User` entity and `UserRepository`. All user management is now handled through Keycloak via `KeycloakUserService`.

## Files Removed

### DTOs
- ✅ `UserCreateRequest.java` - Used for creating users in database
- ✅ `UserUpdateRequest.java` - Used for updating users in database

### Controllers
- ✅ `AuthController.java` - Authentication controller using database users

### Services
- ✅ `MultiTenantAuthService.java` - Multi-tenant auth using database users

## Remaining Portal Files (Not Changed)

These files use `PortalUserRepository` which is different from the removed `UserRepository`. Portal users are temporary registration records, not EHR users:

### Portal Controllers (Keep):
- ✅ `PortalAuthController.java` - Portal registration/login
- ✅ `PortalDemographicsController.java` - Portal demographics
- ✅ `PortalAppointmentController.java` - Portal appointments
- ✅ `PortalProfileController.java` - Portal profile
- ✅ `PortalController.java` - Portal main controller
- ✅ `PortalReviewController.java` - Portal review
- ✅ `PortalHealthController.java` - Portal health records

**Note:** These controllers manage portal registration workflow. When a portal user is approved, `PortalApprovalService` creates the actual user in Keycloak (not database).

## User Management Flow

### Before (Database):
```
User Registration
    ↓
Create User in Database (UserRepository)
    ↓
Create UserOrgRole records
    ↓
User can login
```

### After (Keycloak):
```
Portal Registration
    ↓
Create PortalUser (temporary)
    ↓
Admin Approval
    ↓
Create User in Keycloak (KeycloakUserService)
    ↓
Add to Group & Assign Roles
    ↓
User can login via Keycloak
```

## KeycloakUserService Usage

### Create User:
```java
@Autowired
private KeycloakUserService keycloakUserService;

// Create user with attributes
String userId = keycloakUserService.createUser(
    "user@example.com",
    "John",
    "Doe",
    "password123",
    Map.of(
        "phoneNumber", "+1-555-0100",
        "orgId", "1",
        "dateOfBirth", "1990-01-15"
    )
);
```

### Add to Group:
```java
// Add user to tenant group
keycloakUserService.addUserToGroup(userId, "/Tenants/practice_1");
```

### Assign Roles:
```java
// Assign roles
keycloakUserService.assignRolesToUser(userId, List.of("patient", "portal_user"));
```

### Delete User:
```java
// Delete user from Keycloak
keycloakUserService.deleteUser(userId);
```

## Services Using KeycloakUserService

### ✅ PortalApprovalService
- Creates users in Keycloak when portal users are approved
- Adds users to tenant groups
- Assigns appropriate roles

### Future Services:
- User management admin interface
- Bulk user import
- User synchronization
- Role management

## Database Tables

### Removed Tables:
- ❌ `users` - Removed (now in Keycloak)
- ❌ `user_org_roles` - Removed (now Keycloak groups/roles)

### Portal Tables (Still Used):
- ✅ `portal_users` - Temporary registration data
- ✅ `portal_patients` - Portal patient information
- ✅ `portal_pending_updates` - Pending profile updates

### Tenant Tables (Still Used):
- ✅ `patients` - EHR patient records (in tenant schemas)
- ✅ `appointments` - Appointments
- ✅ `encounters` - Encounters
- ✅ ... (all other EHR data)

## Authentication Flow

### Before (Database):
```
1. User submits credentials
2. AuthController validates against database
3. Generate JWT token
4. Return token to user
```

### After (Keycloak):
```
1. User submits credentials to Keycloak
2. Keycloak validates credentials
3. Keycloak generates JWT token
4. Return token to user
5. Application validates JWT signature
```

## Configuration

### Keycloak Configuration:
```yaml
keycloak:
  auth-server-url: ${KEYCLOAK_URL}
  realm: ciyex
  resource: ciyex-app
  credentials:
    secret: ${KEYCLOAK_CLIENT_SECRET}
  use-resource-role-mappings: true
```

### No More User Database Config:
```yaml
# REMOVED - No longer needed
# spring:
#   jpa:
#     properties:
#       hibernate:
#         default_schema: public  # For users table
```

## Benefits

### ✅ Simplified Architecture
- No user tables in database
- No user CRUD operations
- No password management code
- No user-role mapping tables

### ✅ Better Security
- Keycloak handles authentication
- Centralized password policies
- MFA support out of the box
- Audit logging built-in

### ✅ Easier Maintenance
- No user migration scripts
- No password reset flows
- No email verification code
- No session management

### ✅ Scalability
- Keycloak handles user load
- Distributed sessions
- SSO support
- Federation support

## Migration Checklist

### ✅ Completed:
- [x] Remove User entity
- [x] Remove UserRepository
- [x] Remove UserService
- [x] Remove UserOrgRole entity
- [x] Remove UserOrgRoleRepository
- [x] Create KeycloakUserService
- [x] Create KeycloakOrgService
- [x] Update PortalApprovalService
- [x] Remove UserCreateRequest
- [x] Remove UserUpdateRequest
- [x] Remove AuthController
- [x] Remove MultiTenantAuthService

### ⏳ Remaining:
- [ ] Remove @RequireScope annotations (or update)
- [ ] Remove JwtTokenUtil references
- [ ] Test portal registration flow
- [ ] Test user approval flow
- [ ] Test Keycloak authentication
- [ ] Drop user tables from database
- [ ] Update documentation

## Testing

### Test Portal Registration:
```bash
# 1. Register portal user
POST /api/portal/register
{
  "email": "test@example.com",
  "firstName": "Test",
  "lastName": "User",
  "password": "password123",
  "phoneNumber": "+1-555-0100",
  "dateOfBirth": "1990-01-15",
  "orgId": 1
}

# 2. Admin approves
POST /api/portal/approve/{portalUserId}

# 3. Verify user in Keycloak
# Check Keycloak admin console

# 4. User logs in
POST /realms/ciyex/protocol/openid-connect/token
{
  "grant_type": "password",
  "client_id": "ciyex-app",
  "username": "test@example.com",
  "password": "password123"
}
```

## Rollback Plan

If needed, user tables are in git history:
```bash
# View removed files
git log --diff-filter=D --summary | grep User

# Restore a file
git checkout <commit-hash> -- path/to/file
```

## Next Steps

1. **Remove remaining User references** in portal controllers (if any)
2. **Update @RequireScope annotations** to use Keycloak roles
3. **Remove JwtTokenUtil** or update to use Keycloak tokens
4. **Test end-to-end** portal registration and approval
5. **Drop database tables** once verified working
6. **Update API documentation** to reflect Keycloak auth

---

**Status**: User references removed ✅  
**User Management**: Now in Keycloak  
**Next**: Test and remove remaining auth code
