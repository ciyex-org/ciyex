# Practice Selection - Complete Implementation

## ✅ What's Been Implemented

### 1. **Search Functionality** ✅
- Search bar appears when user has more than 5 practices
- Real-time filtering as you type
- Clear button to reset search
- Shows count of filtered results
- Empty state when no matches found

### 2. **Smart Routing Logic** ✅

The callback page (`callback/page.tsx`) now implements smart routing:

```typescript
if (tenantsData.requiresSelection) {
    // Multiple tenants → redirect to practice selection
    router.push("/select-practice");
} else if (tenantsData.tenants.length === 1) {
    // Single tenant → auto-select and go to dashboard
    setSelectedTenant(tenantsData.tenants[0]);
    router.push("/dashboard");
} else {
    // No tenants or full access → go to dashboard
    router.push("/dashboard");
}
```

### 3. **Backend Logic** ✅

The backend (`TenantController.java`) determines if selection is needed:

```java
@GetMapping("/accessible")
public ResponseEntity<ApiResponse<Map<String, Object>>> getAccessibleTenants() {
    List<String> groups = SecurityContextHolder.getContext()
        .getAuthentication()
        .getAuthorities()
        .stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toList());

    boolean hasFullAccess = tenantAccessService.hasAccessToAllTenants(groups);
    List<String> tenants = tenantAccessService.getAccessibleTenants(groups);
    
    // Require selection if user has multiple tenants and no full access
    boolean requiresSelection = !hasFullAccess && tenants.size() > 1;
    
    Map<String, Object> response = new HashMap<>();
    response.put("hasFullAccess", hasFullAccess);
    response.put("tenants", tenants);
    response.put("requiresSelection", requiresSelection);
    
    return ResponseEntity.ok(ApiResponse.success(response));
}
```

## 📋 User Flow

### Scenario 1: User with Multiple Practices
1. User logs in with `alice@example.com`
2. Backend returns: `requiresSelection: true`, `tenants: ["CareWell", "Qiaben Health"]`
3. Callback redirects to `/select-practice`
4. User sees practice selection page with search (if > 5 practices)
5. User selects a practice
6. Redirected to `/dashboard` with selected tenant

### Scenario 2: User with Single Practice
1. User logs in
2. Backend returns: `requiresSelection: false`, `tenants: ["CareWell"]`
3. Callback auto-selects "CareWell" and redirects to `/dashboard`
4. User goes straight to dashboard (no selection needed)

### Scenario 3: User with Full Access
1. User logs in (e.g., super admin)
2. Backend returns: `hasFullAccess: true`, `requiresSelection: false`
3. Callback redirects to `/dashboard`
4. User can access all tenants without selection

## 🔧 Key Configuration

### Backend Filter Exception
The `/api/tenants/accessible` endpoint is excluded from tenant resolution:

```java
// TenantResolutionFilter.java
private static final String[] EXCLUDED_PATHS = {
    "/api/auth/",
    "/api/portal/auth/",
    "/api/tenants/accessible",  // ✅ Allow users to discover their accessible tenants
    "/actuator/",
    "/error"
};
```

This is **critical** because users need to call this endpoint BEFORE selecting a tenant.

### Keycloak Groups Mapper
Groups are included in JWT tokens via a protocol mapper:

```json
{
  "name": "groups",
  "protocol": "openid-connect",
  "protocolMapper": "oidc-group-membership-mapper",
  "config": {
    "full.path": "true",
    "id.token.claim": "true",
    "access.token.claim": "true",
    "claim.name": "groups",
    "userinfo.token.claim": "true"
  }
}
```

## 🎨 UI Features

### Practice Selection Page
- **Responsive grid**: 1 column on mobile, 2 on tablet, 3 on desktop
- **Search bar**: Only shows if > 5 practices
- **Live filtering**: Updates as you type
- **Empty state**: Shows when no matches found
- **Practice cards**: Clickable cards with hover effects
- **Sign out**: Option to sign out and go back to login

### Search Features
- Search icon on the left
- Clear button (X) on the right when typing
- Result count below search bar
- Case-insensitive search
- Searches in practice name

## 📝 Testing

### Test Users

**alice@example.com** (Multiple practices)
- Groups: `/Tenants/CareWell`, `/Tenants/Qiaben Health`
- Expected: See practice selection page

**bob@example.com** (Multiple practices)
- Groups: `/Tenants/Qiaben Health`, `/Tenants/MediPlus`
- Expected: See practice selection page

**carol@example.com** (Multiple practices)
- Groups: `/Tenants/Qiaben Health`, `/Tenants/CareWell`
- Expected: See practice selection page

### Manual Test
```bash
# 1. Login with alice@example.com
# 2. Should redirect to /select-practice
# 3. Should see 2 practice cards
# 4. Click on a practice
# 5. Should redirect to /dashboard
# 6. localStorage should have selectedTenant set
```

### Automated Test
```bash
cd /home/siva/git/ciyex/scripts
source /home/siva/git/ciyex/.venv/bin/activate
python3 test_practice_selection.py
```

## 🔍 Debugging

If practice selection doesn't show:

1. **Check browser console** for logs:
   - "✅ Callback received data from backend"
   - "Checking accessible tenants for user..."
   - "User has multiple tenants, redirecting to practice selection"

2. **Check localStorage**:
   ```javascript
   localStorage.getItem('groups')  // Should have tenant groups
   localStorage.getItem('selectedTenant')  // Should be null initially
   ```

3. **Check backend logs** for:
   - Request to `/api/tenants/accessible`
   - Response with `requiresSelection: true`

4. **Verify filter exclusion**:
   - `/api/tenants/accessible` should NOT require X-Tenant-Name header
   - Should return 200, not 401

## 🚀 Next Steps (Optional Enhancements)

1. **Remember last selected practice**: Store in localStorage and pre-select
2. **Practice switching**: Add dropdown in header to switch practices
3. **Practice metadata**: Show practice address, phone, etc. on cards
4. **Favorites**: Let users mark favorite practices for quick access
5. **Recent practices**: Show recently accessed practices first
6. **Practice groups**: Group practices by region/type
7. **Keyboard navigation**: Arrow keys to navigate, Enter to select

## 📦 Files Modified

### Backend
- `AuthController.java` - Extract groups from JWT token
- `TenantController.java` - Added `/api/tenants/accessible` endpoint
- `TenantResolutionFilter.java` - Excluded `/api/tenants/accessible` from filter

### Frontend
- `callback/page.tsx` - Added practice selection check
- `PracticeSelection.tsx` - Added search functionality
- `select-practice/page.tsx` - Practice selection page
- `tenantService.ts` - Tenant utilities

### Scripts
- `configure_keycloak_groups_mapper.py` - Configured Keycloak
- `test_practice_selection.py` - Selenium test with improvements

## ✅ Summary

The practice selection feature is now **fully functional**:

✅ Multi-tenant users see practice selection page  
✅ Single-tenant users go straight to dashboard  
✅ Search works for users with many practices  
✅ Backend properly determines if selection is needed  
✅ Groups are extracted from JWT tokens  
✅ Filter exclusion allows endpoint access without tenant  

Everything is working as expected! 🎉
