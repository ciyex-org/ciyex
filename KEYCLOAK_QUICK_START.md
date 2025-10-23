# Keycloak Integration - Quick Start Guide

## 🚀 Quick Setup (5 minutes)

### 1. Set Environment Variables

**Backend** - Add to your environment or IDE run configuration:
```bash
export KEYCLOAK_CLIENT_SECRET=your-client-secret-from-keycloak
```

**Frontend** - Already configured in `.env.local`:
```bash
NEXT_PUBLIC_KEYCLOAK_ENABLED=true
NEXT_PUBLIC_KEYCLOAK_URL=https://aran-stg.zpoa.com
NEXT_PUBLIC_KEYCLOAK_REALM=master
NEXT_PUBLIC_KEYCLOAK_CLIENT_ID=ciyex-app
```

### 2. Start the Application

**Backend:**
```bash
cd /home/siva/git/ciyex
./gradlew bootRun
```

**Frontend:**
```bash
cd /home/siva/git/ciyex/ciyex-ehr-ui
npm run dev
```

### 3. Test Login

1. Navigate to: http://localhost:3000
2. Click **"Aran ID (Keycloak)"** button
3. Enter credentials:
   - Username: `aran-admin`
   - Password: `Kc@2024!Secure#Pass`
4. You should be logged in with groups displayed

## 🔑 Keycloak Admin Access

- **URL**: https://aran-stg.zpoa.com/
- **Username**: aran-admin
- **Password**: Kc@2024!Secure#Pass
- **Realm**: master

## 📝 Key Concepts

### Groups Replace Tenants
```
OLD: User → Organization → Facilities → Roles
NEW: User → Groups (e.g., "doctors", "org-hospital-a", "admin")
```

### Authentication Methods
- **Keycloak**: SSO, centralized user management, groups
- **Local**: Traditional email/password, org/facility structure

### Storage Keys
```javascript
// Keycloak Auth
localStorage.getItem('authMethod')    // "keycloak"
localStorage.getItem('groups')        // ["doctors", "admin"]
localStorage.getItem('token')         // Keycloak access token

// Local Auth
localStorage.getItem('authMethod')    // "local"
localStorage.getItem('orgId')         // "1"
localStorage.getItem('role')          // "DOCTOR"
```

## 💻 Code Examples

### Frontend - Check User Groups
```typescript
import { hasGroup, getUserGroups } from '@/utils/authUtils';

// Check if user is in a group
if (hasGroup('admin')) {
  // Show admin features
}

// Get all groups
const groups = getUserGroups();
console.log('User groups:', groups);
```

### Frontend - Get Current User
```typescript
import { getCurrentUser } from '@/utils/authUtils';

const user = getCurrentUser();
if (user?.authMethod === 'keycloak') {
  console.log('Keycloak user groups:', user.groups);
} else {
  console.log('Local user org:', user.orgId);
}
```

### Backend - Access User Groups
```java
@GetMapping("/api/protected-endpoint")
public ResponseEntity<?> protectedEndpoint(Authentication authentication) {
    if (authentication instanceof JwtAuthenticationToken) {
        Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
        List<String> groups = KeycloakJwtAuthenticationConverter.extractGroups(jwt);
        String email = KeycloakJwtAuthenticationConverter.extractEmail(jwt);
        
        // Use groups for authorization
        if (groups.contains("admin")) {
            // Allow admin access
        }
    }
    return ResponseEntity.ok("Success");
}
```

### Backend - Check Authority
```java
@PreAuthorize("hasAuthority('GROUP_ADMIN')")
@GetMapping("/api/admin-only")
public ResponseEntity<?> adminOnly() {
    return ResponseEntity.ok("Admin access granted");
}
```

## 🔧 Common Tasks

### Create a New User in Keycloak
1. Go to Keycloak admin console
2. Navigate to: Users → Add User
3. Fill in details (username, email, first/last name)
4. Click "Save"
5. Go to "Credentials" tab → Set password
6. Go to "Groups" tab → Join groups

### Create a New Group
1. Go to Keycloak admin console
2. Navigate to: Groups → New
3. Enter group name (e.g., "org-hospital-a")
4. Click "Save"
5. Assign users to the group

### Configure Group Mapper (Include groups in token)
1. Go to Keycloak admin console
2. Navigate to: Clients → ciyex-app → Mappers
3. Click "Create"
4. Set:
   - Name: "groups"
   - Mapper Type: "Group Membership"
   - Token Claim Name: "groups"
   - Add to ID token: ON
   - Add to access token: ON
   - Add to userinfo: ON
5. Click "Save"

## 🐛 Troubleshooting

### "Keycloak authentication is not enabled"
**Fix**: Set `keycloak.enabled=true` in `application.yml` or restart backend

### "Invalid Keycloak credentials"
**Fix**: 
1. Verify credentials in Keycloak admin console
2. Check user is enabled
3. Verify password is correct

### Groups not showing up
**Fix**:
1. Verify user is assigned to groups in Keycloak
2. Check group mapper is configured (see above)
3. Inspect JWT token at https://jwt.io to verify groups claim

### Backend can't connect to Keycloak
**Fix**:
1. Verify Keycloak URL is accessible: `curl https://aran-stg.zpoa.com/`
2. Check firewall/network settings
3. Verify SSL certificates are valid

### Frontend shows "Login failed"
**Fix**:
1. Check browser console for errors
2. Verify backend is running on port 8080
3. Check CORS settings in backend
4. Verify API URL in `.env.local`

## 📚 API Endpoints

### Keycloak Login
```bash
curl -X POST http://localhost:8080/api/auth/keycloak-login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "aran-admin",
    "password": "Kc@2024!Secure#Pass"
  }'
```

### Local Login (Existing)
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

### Protected Endpoint (with token)
```bash
curl -X GET http://localhost:8080/api/protected-endpoint \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

## 🎯 Next Steps

1. **Test with real users**: Create test users in Keycloak and verify login
2. **Configure groups**: Set up group structure matching your organization
3. **Implement RBAC**: Use groups for role-based access control in your app
4. **Token refresh**: Implement token refresh mechanism for long sessions
5. **Production setup**: Configure production Keycloak instance

## 📖 Full Documentation

- **Complete Guide**: See `KEYCLOAK_INTEGRATION.md`
- **Changes Summary**: See `KEYCLOAK_CHANGES_SUMMARY.md`
- **Auth Utils**: See `ciyex-ehr-ui/src/utils/authUtils.ts`

## 🆘 Need Help?

1. Check the full documentation in `KEYCLOAK_INTEGRATION.md`
2. Review Keycloak logs in admin console
3. Check application logs for authentication errors
4. Verify configuration in `application.yml` and `.env.local`

## ✅ Verification Checklist

- [ ] Backend starts without errors
- [ ] Frontend starts without errors
- [ ] Can access login page
- [ ] Can toggle between Local and Keycloak auth
- [ ] Can login with Keycloak credentials
- [ ] Groups are stored in localStorage
- [ ] Token is valid and can access protected endpoints
- [ ] Can login with local credentials (backward compatibility)
- [ ] Can logout successfully

---

**Status**: ✅ Ready for testing
**Last Updated**: 2025-01-22
