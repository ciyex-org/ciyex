# orgId Removal Guide - Single-Tenant Migration

## Overview
This guide documents the systematic removal of `orgId` parameters from the codebase as part of the single-tenant architecture migration.

## What We've Completed

### ✅ 1. Authentication & Practice Selection
- Created `TenantController` with `/api/tenants/accessible` endpoint
- JWT authentication extracts tenant names from Keycloak groups
- Practice selection page works for multi-tenant users

### ✅ 2. Entities - orgId Field Removed
- `PatientClaim` - field and getter/setter removed
- `Invoice` - field removed
- `PatientInsuranceRemitLine` - field and getter/setter removed
- `PatientAccountCredit` - field and getter/setter removed
- `PatientInvoice` - field removed

### ✅ 3. Services - orgId Checks Removed
- `PatientService` - all getCurrentOrgId() calls removed
- `TemplateService` - all getCurrentOrgId() calls removed
- `ScheduleService` - all getCurrentOrgIdOrThrow() calls removed
- `InventoryService` - all getCurrentOrgId() calls removed
- `OrderService` - all getCurrentOrgId() calls removed

### ✅ 4. Logging - Tenant Names Instead of orgId
- `ProviderService` - 15 log statements updated to use tenant names
- `DocumentService` - encryption log updated
- Pattern: `RequestContext.get().getTenantName()`

### ✅ 5. Controllers - orgId Parameters Removed
- `DocumentController` - all orgId path variables and parameters removed
  - URL changed from `/api/{orgId}/patients/{patientId}/documents` 
  - To: `/api/patients/{patientId}/documents`

## Remaining Work

### 🔄 Controllers with orgId Parameters (500+ occurrences)

The following controllers still have `orgId` in their URLs and method parameters:

**Pattern to Fix:**
```java
// BEFORE:
@RequestMapping("/api/{orgId}/patients/{patientId}/...")
public ResponseEntity<?> method(@PathVariable Long orgId, ...) {
    service.method(orgId, ...);
}

// AFTER:
@RequestMapping("/api/patients/{patientId}/...")
public ResponseEntity<?> method(...) {
    service.method(null, ...); // or remove orgId parameter from service
}
```

**Controllers Needing Updates:**
- `AssessmentController` - 14 results
- `AssignedProviderController` - 14 results
- `ChiefComplaintController` - 14 results
- `CodeController` - 14 results
- `CodeTypeController` - 12 results
- `CommunicationController` - 8 results
- `CoverageController` - 8 results
- `DateTimeFinalizedController` - 14 results
- `EncounterBrowseController` - 2 results
- `EncounterController` - 16 results
- `EncounterFeeScheduleController` - 22 results
- `FamilyHistoryController` - 14 results
- `GlobalCodeController` - 7 results
- `GpsController` - 2 results
- `HealthcareServiceController` - 11 results
- `HistoryOfPresentIllnessController` - 14 results
- `InventorySettingsController` - 4 results
- `InvoiceController` - 12 results
- `LabOrderController` - 21 results
- `MedicalProblemController` - 8 results
- `PastMedicalHistoryController` - 14 results
- `PatientBillingController` - 88 results
- `PatientCodeListController` - 17 results
- `PatientMedicalHistoryController` - 14 results
- `PhysicalExamController` - 14 results
- `PlanController` - 14 results
- `ProcedureController` - 12 results
- `ProviderController` - 2 results
- `ProviderNoteController` - 14 results
- `ProviderSignatureController` - 14 results
- `ReviewOfSystemController` - 14 results
- `SignoffController` - 14 results
- `SocialHistoryController` - 12 results
- `VitalsController` - 12 results

### 🔄 Services with orgId Parameters

Many service methods still accept `orgId` as a parameter. These need to be updated to either:
1. Remove the parameter entirely
2. Get tenant info from `RequestContext.get().getTenantName()`

**Example Services:**
- `PatientBillingService` - has setOrgId() calls
- `InvoiceService` - has setOrgId() calls
- `DocumentService` - methods accept orgId parameter

### 🔄 Database Migrations Needed

Create Flyway migrations to drop `org_id` columns from tables:
```sql
-- Example migration
ALTER TABLE patient_claims DROP COLUMN org_id;
ALTER TABLE invoice DROP COLUMN org_id;
ALTER TABLE patient_insurance_remit_lines DROP COLUMN org_id;
ALTER TABLE patient_account_credits DROP COLUMN org_id;
ALTER TABLE patient_invoices DROP COLUMN org_id;
```

### 🔄 Annotation Cleanup

The `@TenantOperation` annotation is no longer needed:
- File: `/src/main/java/com/qiaben/ciyex/annotation/TenantOperation.java`
- Can be deleted (not currently in use)

## Automated Approach

Given the large number of occurrences (500+), consider using:

### Option 1: Find & Replace with Regex
```bash
# Remove @PathVariable Long orgId from method signatures
find . -name "*.java" -type f -exec sed -i 's/@PathVariable Long orgId,\s*//g' {} \;
find . -name "*.java" -type f -exec sed -i 's/@PathVariable Long orgId)/)/' {} \;

# Remove 
find . -name "*.java" -type f -exec sed -i 's/,\s*//g' {} \;
find . -name "*.java" -type f -exec sed -i 's/)/)/' {} \;

# Remove {orgId} from URL paths
find . -name "*.java" -type f -exec sed -i 's/{orgId}\/patients/patients/g' {} \;
```

### Option 2: IDE Refactoring
1. Use IntelliJ's "Structural Search and Replace"
2. Search for pattern: `@PathVariable Long orgId`
3. Replace with empty string
4. Review and apply changes

### Option 3: Manual Controller-by-Controller
Fix each controller individually (safest but most time-consuming)

## Testing After Changes

1. **Unit Tests**: Update tests to remove orgId parameters
2. **Integration Tests**: Update API calls to use new URL patterns
3. **Frontend**: Update all API calls to remove orgId from URLs
4. **Postman/API Docs**: Update documentation

## Migration Strategy

### Phase 1: Backend (Current)
- ✅ Remove orgId from entities
- ✅ Remove orgId checks from services
- ✅ Update logging to use tenant names
- 🔄 Remove orgId from controllers (IN PROGRESS)
- 🔄 Remove orgId from service method signatures

### Phase 2: Database
- Create Flyway migrations to drop org_id columns
- Test migrations on staging environment

### Phase 3: Frontend
- Update all API calls to remove orgId from URLs
- Update routing to remove orgId path segments
- Test all features

### Phase 4: Cleanup
- Remove unused orgId-related code
- Remove @TenantOperation annotation
- Update API documentation

## Notes

- **Backward Compatibility**: If you need to maintain backward compatibility, consider keeping old endpoints and adding new ones without orgId
- **Tenant Context**: Use `RequestContext.get().getTenantName()` to get current tenant
- **Null Safety**: When passing `null` for orgId to services, ensure services handle it properly

## Progress Tracking

- Entities: 5/5 (100%)
- Services: 5/~30 (17%)
- Controllers: 1/~35 (3%)
- Logging: 2/~10 (20%)
- Database: 0/5 (0%)

**Estimated Remaining Work**: ~500 occurrences across controllers and services
