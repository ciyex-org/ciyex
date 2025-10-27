# Services Requiring Manual Fixes

Based on the search results, here are the services that need attention:

## 1. AllergyIntoleranceService.java ✅ DONE
- Fixed repository calls
- Added new repository methods without org_id filter

## 2. CoverageService.java (1 result)
- `getCurrentOrgIdOrThrow()` method - likely unused, can be removed

## 3. DocumentService.java (2 results)
- Line 55: Error message references orgId
- Line 105: S3 key path uses orgId variable

## 4. DocumentSettingsService.java (2 results)
- `getByOrgId()` method - rename to `get()`
- `deleteByOrgId()` method - rename to `delete()`

## 5. EncounterFeeScheduleService.java (4 results)
- Multiple `verifyScope(s, orgId, ...)` calls
- Need to remove orgId parameter from verifyScope

## 6. EncounterService.java (3 results)
- `updateStatus(id, patientId, orgId, status)` calls
- Remove orgId parameter

## 7. HealthcareServiceService.java (1 result)
- `getByOrgId()` method - rename to `getAll()`

## 8. KeycloakAuthService.java (1 result)
- `getOrgIdFromGroupAttributes()` method - used by TenantAccessService

## 9. LabOrderService.java (14 results)
- Complex multi-tenant logic with `allowedOrgIds`
- May need significant refactoring or can be left as-is

## 10. ListOptionService.java (2 results)
- `entity.setOrgId(RequestContext.get().getTenantName())`
- `dto.setOrgId(entity.getOrgId())`

## 11. PatientCodeListService.java (3 results)
- `setSearchPath()` method uses orgId
- `saveBulk()` sets `r.orgId = orgId`

## 12. PatientService.java (3 results)
- `getCurrentOrgId()` - unused, remove
- `tenantNameFromOrgId()` - refactor to use tenant name directly

## 13. TenantAccessService.java (16 results)
- `requiresOrgIdHeader()`, `getOrgIdFromGroupAttributes()`, `resolveOrgId()`
- Core tenant resolution logic - may need to keep for group attribute mapping

## 14. TenantProvisionService.java (2 results)
- `provisionTenantFromTemplate(String orgId, ...)` 
- Schema provisioning - may need orgId for schema names

---

## Quick Fix Strategy

For each service:
1. Check if method is unused → Remove it
2. Check if it's a rename → Rename method
3. Check if it's logic → Update to use tenant name from RequestContext
4. Check if it's multi-tenant → Document or simplify

## Priority Order

**High Priority** (breaks compilation):
1. ✅ AllergyIntoleranceService
2. DocumentService
3. EncounterFeeScheduleService
4. EncounterService

**Medium Priority** (method renames):
5. DocumentSettingsService
6. HealthcareServiceService
7. PatientService

**Low Priority** (can work as-is):
8. LabOrderService (multi-tenant)
9. TenantAccessService (group mapping)
10. TenantProvisionService (schema provisioning)
11. KeycloakAuthService (auth logic)
12. ListOptionService
13. PatientCodeListService
