# @RequireScope Removed - Using Spring Security @PreAuthorize

## Summary

Removed custom `@RequireScope` annotations and replaced with Spring Security's standard `@PreAuthorize` annotation. Authorization now uses Keycloak roles instead of custom scope system.

## Changes Made

### ✅ Controllers Updated:

1. **UserController** - Uses KeycloakUserService
2. **PatientController** - Uses @PreAuthorize
3. **AppointmentController** - Uses @PreAuthorize  
4. **OrdersController** - Uses @PreAuthorize

### Before (Custom Scope System):
```java
@RestController
@RequireScope("patients:read")
public class PatientController {
    
    @PostMapping
    @RequireScope("patients:write")
    public ResponseEntity<?> create(@RequestBody PatientDto dto) {
        // ...
    }
}
```

### After (Spring Security):
```java
@RestController
public class PatientController {
    
    @PostMapping
    @PreAuthorize("hasAnyRole('admin', 'provider')")
    public ResponseEntity<?> create(@RequestBody PatientDto dto) {
        // ...
    }
}
```

## Authorization Mapping

### Old Scopes → New Roles:

| Old Scope | New Authorization |
|-----------|------------------|
| `patients:read` | `hasAnyRole('admin', 'provider', 'patient')` |
| `patients:write` | `hasAnyRole('admin', 'provider')` |
| `appointments:read` | `hasAnyRole('admin', 'provider', 'patient')` |
| `appointments:write` | `hasAnyRole('admin', 'provider', 'patient')` |
| `labs:read` | `hasAnyRole('admin', 'provider')` |
| `labs:write` | `hasAnyRole('admin', 'provider')` |
| `user:read` | `hasAnyRole('admin', 'provider')` |
| `user:write` | `hasAnyRole('admin', 'provider')` |

## Keycloak Roles

### Realm Roles:
- **admin** - Full system access
- **provider** - Healthcare provider access
- **patient** - Patient portal access
- **staff** - Staff/receptionist access

### Role Hierarchy:
```
admin (highest)
  ├─ Can manage all resources
  ├─ Can manage users
  └─ Can configure system

provider
  ├─ Can manage patients
  ├─ Can create/update appointments
  ├─ Can order labs
  └─ Can view medical records

patient
  ├─ Can view own appointments
  ├─ Can book appointments
  ├─ Can view own medical records
  └─ Can update own profile

staff
  ├─ Can schedule appointments
  ├─ Can check-in patients
  └─ Can view schedules
```

## Spring Security Configuration

### Enable Method Security:
```java
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    // Configuration
}
```

### Role Extraction from JWT:
```java
@Bean
public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = 
        new JwtGrantedAuthoritiesConverter();
    
    // Extract roles from Keycloak JWT
    grantedAuthoritiesConverter.setAuthoritiesClaimName("realm_access.roles");
    grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
    
    JwtAuthenticationConverter jwtAuthenticationConverter = 
        new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
        grantedAuthoritiesConverter);
    
    return jwtAuthenticationConverter;
}
```

## @PreAuthorize Examples

### Role-Based:
```java
// Single role
@PreAuthorize("hasRole('admin')")
public void adminOnly() { }

// Multiple roles (OR)
@PreAuthorize("hasAnyRole('admin', 'provider')")
public void adminOrProvider() { }

// All roles required (AND)
@PreAuthorize("hasRole('admin') and hasRole('provider')")
public void adminAndProvider() { }
```

### Authentication-Based:
```java
// Any authenticated user
@PreAuthorize("isAuthenticated()")
public void authenticatedOnly() { }

// Anonymous users
@PreAuthorize("isAnonymous()")
public void anonymousOnly() { }
```

### Expression-Based:
```java
// Check user owns resource
@PreAuthorize("#email == authentication.name")
public void updateOwnProfile(String email) { }

// Complex expression
@PreAuthorize("hasRole('admin') or (#userId == authentication.principal.id)")
public void updateUser(Long userId) { }
```

## UserController Special Case

UserController now uses KeycloakUserService but methods return "not implemented" messages:

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final KeycloakUserService keycloakUserService;
    
    @PutMapping("/email/{email}/profile")
    @PreAuthorize("hasAnyRole('admin', 'provider')")
    public ResponseEntity<?> updateProfileByEmail(...) {
        return ResponseEntity.ok(Map.of(
            "message", "Profile update not yet implemented for Keycloak users",
            "note", "Users are now managed in Keycloak"
        ));
    }
}
```

**Why?** User management should be done through:
1. **Keycloak Admin Console** - For admins
2. **Keycloak Account Management** - For users
3. **KeycloakUserService** - For programmatic access

## Benefits

### ✅ Standard Spring Security
- Uses industry-standard annotations
- Better IDE support
- More documentation available
- Easier for new developers

### ✅ Keycloak Integration
- Roles come from Keycloak JWT
- Centralized role management
- No custom authorization code
- SSO support

### ✅ Simpler Codebase
- Removed custom @RequireScope
- Removed scope validation logic
- Removed scope configuration
- Less code to maintain

### ✅ More Flexible
- Can use complex expressions
- Can check multiple conditions
- Can access authentication object
- Can validate method parameters

## Testing

### Get JWT Token:
```bash
curl -X POST "https://keycloak.example.com/realms/ciyex/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=ciyex-app" \
  -d "username=provider@example.com" \
  -d "password=password123"
```

### Call API with Token:
```bash
curl -X GET "https://api.example.com/api/patients" \
  -H "Authorization: Bearer <access_token>"
```

### Verify Roles in JWT:
```bash
# Decode JWT at jwt.io
{
  "realm_access": {
    "roles": ["provider", "user"]
  },
  "resource_access": {
    "ciyex-app": {
      "roles": ["patient-access"]
    }
  }
}
```

## Migration Checklist

### ✅ Completed:
- [x] Remove @RequireScope from UserController
- [x] Remove @RequireScope from PatientController
- [x] Remove @RequireScope from AppointmentController
- [x] Remove @RequireScope from OrdersController
- [x] Replace with @PreAuthorize
- [x] Update UserController to use KeycloakUserService

### ⏳ Remaining:
- [ ] Remove @RequireScope from portal controllers
- [ ] Remove @RequireScope from TelehealthController
- [ ] Remove JwtTokenUtil (if not needed)
- [ ] Remove PortalAuthService (if not needed)
- [ ] Configure Spring Security for Keycloak
- [ ] Test authorization with different roles
- [ ] Update API documentation

## Keycloak Role Assignment

### Assign Roles to Users:
```
Keycloak Admin Console →
  Users → Select User → Role Mapping →
    Assign Realm Roles: admin, provider, patient
```

### Assign Roles to Groups:
```
Keycloak Admin Console →
  Groups → /Tenants/practice_1 → Role Mapping →
    Assign Realm Roles: provider, patient
```

### Default Roles:
```
Keycloak Admin Console →
  Realm Settings → User Registration →
    Default Roles: patient, user
```

## Troubleshooting

### Error: "Access Denied"
**Solution:** Check user has required role in Keycloak

### Error: "hasRole() not working"
**Solution:** Ensure role prefix is "ROLE_" in JWT converter

### Error: "@PreAuthorize not evaluated"
**Solution:** Enable method security with `@EnableMethodSecurity`

### Error: "Roles not in JWT"
**Solution:** Check Keycloak client mappers include realm roles

## Next Steps

1. **Remove remaining @RequireScope** from portal controllers
2. **Configure Spring Security** for Keycloak JWT validation
3. **Test authorization** with different user roles
4. **Update documentation** with new authorization model
5. **Train team** on Spring Security annotations

---

**Status**: @RequireScope removed ✅  
**Authorization**: Now using Spring Security @PreAuthorize  
**Roles**: Loaded from Keycloak JWT  
**Next**: Remove from remaining controllers and configure Spring Security
