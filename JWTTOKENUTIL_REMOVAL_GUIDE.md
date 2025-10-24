# JwtTokenUtil Removal Guide

## Summary

Removed `JwtTokenUtil` and replaced with Spring Security's `Authentication` object. JWT tokens are now validated by Spring Security/Keycloak adapter automatically.

## Changes Required

### Before (JwtTokenUtil):
```java
@RestController
@RequiredArgsConstructor
public class PortalController {
    
    private final JwtTokenUtil jwtTokenUtil;
    
    @GetMapping("/endpoint")
    public ResponseEntity<?> endpoint(HttpServletRequest request) {
        String token = resolveToken(request);
        Long userId = jwtTokenUtil.getUserIdFromToken(token);
        // ... use userId
    }
    
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

### After (Spring Security Authentication):
```java
@RestController
@RequiredArgsConstructor
public class PortalController {
    
    @GetMapping("/endpoint")
    public ResponseEntity<?> endpoint(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        String email = authentication.getName(); // Get email from JWT
        // TODO: Lookup user by email if needed
        // ... use email or userId
    }
}
```

## Files to Update

### ✅ Completed:
- [x] PortalPatientController.java

### ⏳ Remaining Portal Controllers:
- [ ] PortalController.java
- [ ] PortalProviderController.java
- [ ] PortalLocationController.java
- [ ] PortalHealthController.java
- [ ] PortalReviewController.java
- [ ] PortalListOptionsController.java
- [ ] PortalAppointmentController.java
- [ ] PortalProfileController.java

### ⏳ Other Controllers:
- [ ] TelehealthController.java

### ⏳ Missing Services:
- [ ] PortalAuthService.java (needs creation or removal)

## Step-by-Step Fix

### 1. Remove JwtTokenUtil Import:
```java
// Remove this:
import com.qiaben.ciyex.util.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;

// Add this:
import org.springframework.security.core.Authentication;
```

### 2. Remove JwtTokenUtil Field:
```java
// Remove this:
private final JwtTokenUtil jwtTokenUtil;
```

### 3. Update Method Signatures:
```java
// Change from:
public ResponseEntity<?> method(HttpServletRequest request) {

// To:
public ResponseEntity<?> method(Authentication authentication) {
```

### 4. Replace Token Extraction:
```java
// Change from:
String token = resolveToken(request);
Long userId = jwtTokenUtil.getUserIdFromToken(token);

// To:
if (authentication == null || !authentication.isAuthenticated()) {
    return ResponseEntity.status(401).body("Unauthorized");
}
String email = authentication.getName();
// TODO: Get userId from email
```

### 5. Remove resolveToken Method:
```java
// Remove this entire method:
private String resolveToken(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
        return bearerToken.substring(7);
    }
    return null;
}
```

## Authentication Object

### What's Available:
```java
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

// Get username/email (from JWT 'sub' or 'preferred_username' claim)
String email = authentication.getName();

// Get authorities/roles
Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

// Check if authenticated
boolean isAuthenticated = authentication.isAuthenticated();

// Get principal (could be UserDetails or String)
Object principal = authentication.getPrincipal();
```

### JWT Claims in Keycloak:
```json
{
  "sub": "user-uuid",
  "email": "user@example.com",
  "preferred_username": "user@example.com",
  "name": "John Doe",
  "given_name": "John",
  "family_name": "Doe",
  "realm_access": {
    "roles": ["patient", "user"]
  }
}
```

## User Lookup Pattern

Since users are now in Keycloak, you need to look up by email:

```java
@Service
@RequiredArgsConstructor
public class PortalService {
    
    private final PortalUserRepository portalUserRepository;
    
    public PortalUser getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return portalUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
```

## Quick Fix Script

For each controller file, apply these changes:

```bash
# 1. Remove JwtTokenUtil import
sed -i '/import com.qiaben.ciyex.util.JwtTokenUtil;/d' *.java

# 2. Remove HttpServletRequest import if only used for JWT
sed -i '/import jakarta.servlet.http.HttpServletRequest;/d' *.java

# 3. Add Authentication import
sed -i '1a import org.springframework.security.core.Authentication;' *.java
```

## Testing

### Get JWT Token:
```bash
curl -X POST "https://keycloak.example.com/realms/ciyex/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=ciyex-app" \
  -d "username=patient@example.com" \
  -d "password=password123"
```

### Call API:
```bash
curl -X GET "https://api.example.com/api/portal/patient/me" \
  -H "Authorization: Bearer <access_token>"
```

### Verify Authentication:
```java
@GetMapping("/test-auth")
public ResponseEntity<?> testAuth(Authentication authentication) {
    if (authentication == null) {
        return ResponseEntity.ok("No authentication");
    }
    
    return ResponseEntity.ok(Map.of(
        "authenticated", authentication.isAuthenticated(),
        "name", authentication.getName(),
        "authorities", authentication.getAuthorities()
    ));
}
```

## Benefits

### ✅ Standard Spring Security
- No custom JWT parsing
- Automatic token validation
- Built-in security features

### ✅ Keycloak Integration
- JWT validated by Keycloak adapter
- Roles extracted automatically
- SSO support

### ✅ Simpler Code
- No JwtTokenUtil class needed
- No manual token extraction
- Less boilerplate

### ✅ Better Security
- Token validation by Spring Security
- Automatic expiration checking
- Signature verification

## Common Issues

### Issue: Authentication is null
**Solution:** Ensure Spring Security is configured to validate JWT tokens

### Issue: Can't get userId
**Solution:** Look up user by email from authentication.getName()

### Issue: Roles not working
**Solution:** Configure JWT converter to extract roles from Keycloak JWT

## Spring Security Configuration

### Required Configuration:
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/portal/**").authenticated()
                .anyRequest().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );
        
        return http.build();
    }
    
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = 
            new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("realm_access.roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        
        JwtAuthenticationConverter jwtAuthenticationConverter = 
            new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
            grantedAuthoritiesConverter);
        
        return jwtAuthenticationConverter();
    }
}
```

## Next Steps

1. **Remove JwtTokenUtil** from all portal controllers
2. **Remove PortalAuthService** or update to use Keycloak
3. **Configure Spring Security** for JWT validation
4. **Test authentication** with Keycloak tokens
5. **Update user lookup** to use email instead of userId

---

**Status**: JwtTokenUtil removal in progress  
**Completed**: PortalPatientController  
**Remaining**: 8 portal controllers + TelehealthController  
**Next**: Batch update remaining controllers
