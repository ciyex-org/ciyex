# PortalApprovalService Updated to Use Keycloak

## Summary

Updated `PortalApprovalService` to create users in Keycloak instead of the database. Portal user approval now creates Keycloak users with proper group membership and roles.

## Changes Made

### ✅ Created KeycloakUserService.java

New service to manage users in Keycloak with methods:

#### User Management:
- `createUser(email, firstName, lastName, password, attributes)` - Create user in Keycloak
- `addUserToGroup(userId, groupPath)` - Add user to tenant group
- `assignRolesToUser(userId, roleNames)` - Assign roles to user
- `deleteUser(userId)` - Delete user from Keycloak

### ✅ Updated PortalApprovalService.java

**Removed Dependencies:**
- ❌ `UserRepository` - No longer needed
- ❌ `PasswordEncoder` - Keycloak handles passwords

**Added Dependencies:**
- ✅ `KeycloakUserService` - Manage users in Keycloak

**Updated Method:**
- `createTenantPatient()` - Now creates user in Keycloak instead of database

## How It Works Now

### Before (Database):
```java
// Create user in database
User tenantUser = User.builder()
    .email(portalUser.getEmail())
    .firstName(portalUser.getFirstName())
    // ... other fields
    .build();
User savedUser = userRepository.save(tenantUser);
```

### After (Keycloak):
```java
// Prepare attributes
Map<String, String> attributes = new HashMap<>();
attributes.put("uuid", portalUser.getUuid());
attributes.put("dateOfBirth", portalPatient.getDateOfBirth().toString());
attributes.put("phoneNumber", portalUser.getPhoneNumber());
attributes.put("orgId", String.valueOf(portalUser.getOrgId()));
// ... other attributes

// Create user in Keycloak
String keycloakUserId = keycloakUserService.createUser(
    portalUser.getEmail(),
    portalUser.getFirstName(),
    portalUser.getLastName(),
    portalUser.getPassword(),
    attributes
);

// Add to tenant group
String tenantGroup = "/Tenants/practice_" + portalUser.getOrgId();
keycloakUserService.addUserToGroup(keycloakUserId, tenantGroup);

// Assign roles
keycloakUserService.assignRolesToUser(keycloakUserId, List.of("patient"));
```

## Portal Approval Workflow

### 1. User Registers on Portal
- User fills out registration form
- Creates `PortalUser` and `PortalPatient` records
- Status: `PENDING`

### 2. Admin Reviews Application
- Admin views pending users
- Checks patient information
- Decides to approve or reject

### 3. Approval Process (Updated)
When admin approves:

**a. Create Keycloak User:**
```java
String userId = keycloakUserService.createUser(
    email, firstName, lastName, password, attributes
);
```

**b. Add to Tenant Group:**
```java
keycloakUserService.addUserToGroup(userId, "/Tenants/practice_1");
```

**c. Assign Patient Role:**
```java
keycloakUserService.assignRolesToUser(userId, List.of("patient"));
```

**d. Create Patient Record:**
```java
Patient patient = patientRepository.save(tenantPatient);
```

**e. Update Portal Status:**
```java
portalUser.setStatus(PortalStatus.APPROVED);
portalPatient.setEhrPatientId(patient.getId());
```

### 4. User Can Now Login
- User logs in via Keycloak
- Has access to tenant group
- Has patient role
- Linked to patient record in EHR

## Keycloak User Structure

### User Attributes:
```
User: patient@example.com
  firstName: John
  lastName: Doe
  email: patient@example.com
  enabled: true
  emailVerified: true
  
  Attributes:
    - uuid: "abc-123-def"
    - dateOfBirth: "1990-01-15"
    - phoneNumber: "+1-555-0100"
    - street: "123 Main St"
    - city: "Springfield"
    - state: "IL"
    - postalCode: "62701"
    - country: "USA"
    - orgId: "1"
  
  Groups:
    - /Tenants/practice_1
  
  Roles:
    - patient
```

## Benefits

### ✅ Centralized User Management
- All users in Keycloak
- Single source of truth
- No database sync issues

### ✅ Better Security
- Keycloak handles password hashing
- Built-in password policies
- MFA support

### ✅ Group-Based Access
- Users automatically get tenant access
- Group membership controls data access
- Easy to manage permissions

### ✅ Role-Based Authorization
- Roles assigned in Keycloak
- Can be used for fine-grained access control
- Supports role hierarchies

## API Methods

### KeycloakUserService API:

```java
// Create user
String userId = keycloakUserService.createUser(
    "user@example.com",      // email
    "John",                  // firstName
    "Doe",                   // lastName
    "password123",           // password
    Map.of(                  // attributes
        "phoneNumber", "+1-555-0100",
        "orgId", "1"
    )
);

// Add to group
keycloakUserService.addUserToGroup(userId, "/Tenants/practice_1");

// Assign roles
keycloakUserService.assignRolesToUser(userId, List.of("patient", "portal_user"));

// Delete user
keycloakUserService.deleteUser(userId);
```

## Database Changes

### Tables Still Used:
- ✅ `portal_users` - Portal registration data
- ✅ `portal_patients` - Portal patient data
- ✅ `patients` - EHR patient records (tenant schema)

### Tables No Longer Used:
- ❌ `users` - Removed (now in Keycloak)
- ❌ `user_org_roles` - Removed (now Keycloak groups)

## Testing

### Manual Test Flow:

1. **Register Portal User:**
```bash
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
```

2. **Admin Approves:**
```bash
POST /api/portal/approve/{portalUserId}
{
  "approvedByUserId": 123
}
```

3. **Verify in Keycloak:**
- Check user exists
- Check group membership
- Check role assignment

4. **User Logs In:**
```bash
POST /auth/realms/ciyex/protocol/openid-connect/token
{
  "grant_type": "password",
  "client_id": "ciyex-app",
  "username": "test@example.com",
  "password": "password123"
}
```

5. **Verify Access:**
- User can access patient portal
- User has correct tenant context
- User can view their patient data

## Configuration Required

### Keycloak Setup:

1. **Create Realm Roles:**
   - `patient`
   - `provider`
   - `admin`
   - `portal_user`

2. **Create Group Structure:**
```
/Tenants
  /practice_1
  /practice_2
  /practice_3
```

3. **Admin Credentials:**
```yaml
keycloak:
  admin:
    username: aran-admin
    password: ${KEYCLOAK_ADMIN_PASSWORD}
```

## Error Handling

### Common Errors:

**User Already Exists:**
```java
try {
    keycloakUserService.createUser(...);
} catch (RuntimeException e) {
    // Handle duplicate user
    log.error("User already exists in Keycloak");
}
```

**Group Not Found:**
```java
// Service logs warning and continues
// Check Keycloak admin console for group structure
```

**Role Not Found:**
```java
// Service throws exception
// Ensure roles are created in Keycloak
```

## Next Steps

1. ✅ PortalApprovalService updated
2. ⏳ Test portal registration flow
3. ⏳ Test approval workflow
4. ⏳ Verify Keycloak user creation
5. ⏳ Test user login
6. ⏳ Update other services to use Keycloak

## Related Files

- ✅ `KeycloakUserService.java` - User management
- ✅ `KeycloakOrgService.java` - Org configuration
- ✅ `PortalApprovalService.java` - Portal approval workflow
- ⏳ Other services that need updating

---

**Status**: PortalApprovalService Updated ✅  
**Users**: Now managed in Keycloak  
**Next**: Test and update other services
