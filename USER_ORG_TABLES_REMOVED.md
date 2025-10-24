# User and Org Tables Removed - Summary

## Files Removed

### Entities
- ✅ User.java
- ✅ UserOrgRole.java  
- ✅ Org.java
- ✅ OrgConfig.java

### Repositories
- ✅ UserRepository.java
- ✅ UserOrgRoleRepository.java
- ✅ OrgRepository.java
- ✅ OrgConfigRepository.java

### Services
- ✅ UserService.java
- ✅ OrganizationAuthService.java
- ✅ CiyexUserDetailsService.java
- ✅ OrgService.java
- ✅ OrgConfigService.java
- ✅ StripeService.java
- ✅ StripeCustomerBackfill.java
- ✅ DocumentSettingsService.java
- ✅ PortalAuthService.java

### Controllers
- ✅ OrgController.java
- ✅ OrgConfigController.java
- ✅ MultiTenantDemoController.java
- ✅ StripeBillingCardController.java

### DTOs
- ✅ OrgDto.java

### Storage
- ✅ ExternalOrgStorage.java
- ✅ FhirExternalOrgStorage.java

### Updated
- ✅ TenantContextInterceptor.java - Simplified to only set orgId from header

## Files Created
- ✅ KeycloakOrgService.java - Load org info from Keycloak

## Remaining Files to Remove/Update

Based on compilation errors, these files still reference User/Org:

1. **PractitionerRoleDto.java** - References Org
2. **OrgIntegrationConfigProvider.java** - References OrgConfig/OrgConfigRepository  
3. **SecurityConfig.java** - References CiyexUserDetailsService
4. **JwtRequestFilter.java** - References CiyexUserDetailsService
5. **UserScopeService.java** - References User/UserOrgRole/UserRepository
6. **SuperAdminConfig.java** - References User
7. **RoleScopeManagementService.java** - References User/UserOrgRole/UserOrgRoleRepository
8. **JwtTokenUtil.java** - Likely references User

## Recommendation

Since the system is being redesigned to use Keycloak for all user and org management, I recommend:

1. **Remove all authentication/authorization classes** that depend on database tables
2. **Use Keycloak JWT tokens** for authentication
3. **Extract user info from JWT claims** instead of database
4. **Extract org/tenant info from Keycloak group attributes**

## Next Steps

Option 1: Remove all remaining files that depend on User/Org tables
Option 2: Update each file to use Keycloak instead of database

Which approach would you prefer?
