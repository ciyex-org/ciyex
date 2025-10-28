# Complete orgId Removal Plan

## Executive Summary

**Comprehensive plan to remove all `orgId` references from the codebase.**

### Current State
- **Backend Java**: ~1,563 occurrences in 199 files
- **Frontend TypeScript**: ~317 occurrences in 41 files
- **Database**: Multiple `org_id` columns
- **Estimated effort**: 20-40 hours

### Status Overview
| Component | Files | Occurrences | Status |
|-----------|-------|-------------|--------|
| Controllers | 54 | ~800 | ⚠️ Partial |
| Services | 86 | ~600 | ⚠️ Partial |
| Storage | 38 | ~111 | ❌ Not started |
| FHIR | 48 | ~150 | ❌ Not started |
| Repositories | 32 | ~80 | ⚠️ Some done |
| Frontend | 41 | ~317 | ❌ Not started |

---

## Phase 1: Backend Controllers

### Scope
54 controllers, ~800 occurrences

### Changes Required
1. Remove `@PathVariable Long orgId`
2. Remove `@RequestHeader("x-org-id") Long orgId`
3. Update paths: `/api/{orgId}/...` → `/api/...`
4. Update service calls

### Top Priority Controllers
- PatientBillingController (88)
- CodeController (30)
- Assessment/Provider/History controllers (26 each)

### Automation Script
```bash
#!/bin/bash
for file in src/main/java/com/qiaben/ciyex/controller/*.java; do
    sed -i 's/@PathVariable Long orgId,\s*//g' "$file"
    sed -i 's/@RequestHeader("x-org-id") Long orgId,\s*//g' "$file"
    sed -i 's|/api/{orgId}/|/api/|g' "$file"
done
```

### Estimated Time: 2-4 hours

---

## Phase 2: Backend Services

### Scope
86 services, ~600 occurrences

### Changes Required
1. Remove `Long orgId` parameters
2. Remove `getCurrentOrgId()` methods
3. Remove `setOrgId()` calls
4. Update repository calls

### Top Priority Services
- PatientBillingService (88)
- AllergyIntoleranceService (28)
- EncounterService (24)
- PatientCodeListService (25)

### Automation Script
```bash
#!/bin/bash
for file in src/main/java/com/qiaben/ciyex/service/*.java; do
    sed -i '/\.setOrgId(/d' "$file"
    sed -i '/private Long getCurrentOrgId/,/^    }$/d' "$file"
done
```

### Estimated Time: 6-9 hours

---

## Phase 3: Storage Layer

### Scope
- 38 storage interfaces
- 48 FHIR implementations

### Changes Required
Update method signatures to remove `Long orgId` parameters

### Example
```java
// Before
HealthcareServiceDto create(HealthcareServiceDto dto, Long orgId);

// After
HealthcareServiceDto create(HealthcareServiceDto dto);
```

### Estimated Time: 4-6 hours

---

## Phase 4: Repositories

### Scope
32 repositories, ~80 occurrences

### Changes Required
1. Remove orgId parameters
2. Update @Query annotations
3. Remove org_id from WHERE clauses

### Example
```java
// Before
@Query("SELECT p FROM Patient p WHERE p.orgId = :orgId AND p.id = :id")
Optional<Patient> findByOrgIdAndId(@Param("orgId") Long orgId, @Param("id") Long id);

// After
@Query("SELECT p FROM Patient p WHERE p.id = :id")
Optional<Patient> findById(@Param("id") Long id);
```

### Estimated Time: 3-4 hours

---

## Phase 5: Entities & DTOs

### Scope
- 5 entities (mostly done)
- 3 DTOs

### Changes Required
Remove `orgId` fields and getters/setters

### Estimated Time: 1-2 hours

---

## Phase 6: Frontend API Calls

### Scope
41 files, ~317 occurrences

### Changes Required
1. Update API URLs: `/api/${orgId}/...` → `/api/...`
2. Remove `x-org-id` headers
3. Update routing

### Top Priority Files
- telehealth/[appointmentId]/page.tsx (36)
- settings/Documents/page.tsx (27)
- Documents.tsx (16)
- authUtils.ts (14)

### Automation Script
```bash
#!/bin/bash
find ciyex-ehr-ui/src -type f \( -name "*.tsx" -o -name "*.ts" \) -exec \
    sed -i 's|/api/\${orgId}/|/api/|g' {} \;
```

### Estimated Time: 8-12 hours

---

## Phase 7: Frontend Utils

### Scope
- authUtils.ts
- fetchWithOrg.ts
- types.ts

### Changes Required
Remove orgId extraction and usage from utilities

### Estimated Time: 2-3 hours

---

## Phase 8: Database Migrations

### Scope
Drop `org_id` columns from all tables

### Migration Template
```sql
-- V{version}__drop_org_id_columns.sql
ALTER TABLE patient_claims DROP COLUMN IF EXISTS org_id;
ALTER TABLE invoice DROP COLUMN IF EXISTS org_id;
ALTER TABLE patient_insurance_remit_lines DROP COLUMN IF EXISTS org_id;
-- Add more tables...
```

### CRITICAL: Backup first!
```bash
pg_dump -U postgres -d ciyex > backup_$(date +%Y%m%d).sql
```

### Estimated Time: 4 hours

---

## Phase 9: Testing

### Test Checklist
- [ ] Backend compiles: `./mvnw clean compile`
- [ ] Frontend builds: `npm run build`
- [ ] Unit tests pass: `./mvnw test`
- [ ] API endpoints work
- [ ] Frontend E2E tests pass
- [ ] Manual smoke tests

### Verification
```bash
# Check remaining occurrences
grep -r "orgId" src/main/java --include="*.java" | wc -l
grep -r "orgId" ciyex-ehr-ui/src | wc -l
```

### Estimated Time: 4-6 hours

---

## Phase 10: Documentation & Cleanup

### Tasks
1. Update API documentation
2. Update README files
3. Remove backup files
4. Update deployment docs
5. Create migration guide for teams

### Estimated Time: 2-3 hours

---

## Total Effort Estimate

| Phase | Time (hours) |
|-------|--------------|
| 1. Controllers | 2-4 |
| 2. Services | 6-9 |
| 3. Storage | 4-6 |
| 4. Repositories | 3-4 |
| 5. Entities/DTOs | 1-2 |
| 6. Frontend API | 8-12 |
| 7. Frontend Utils | 2-3 |
| 8. Database | 4 |
| 9. Testing | 4-6 |
| 10. Documentation | 2-3 |
| **TOTAL** | **36-53 hours** |

---

## Execution Strategy

### Option 1: Big Bang (Recommended for small teams)
- Complete all phases in sequence
- Single large PR
- Requires downtime for deployment

### Option 2: Incremental (Recommended for large teams)
- Phase 1-2: Backend controllers & services
- Phase 3-5: Storage, repos, entities
- Phase 6-7: Frontend
- Phase 8: Database (last)
- Multiple PRs, gradual rollout

### Option 3: Hybrid
- Backend first (Phases 1-5)
- Deploy with backward compatibility
- Frontend next (Phases 6-7)
- Database cleanup last (Phase 8)

---

## Risk Mitigation

### Risks
1. **Breaking changes**: API incompatibility
2. **Data loss**: Database migration errors
3. **Downtime**: Service interruption
4. **Testing gaps**: Missed edge cases

### Mitigation
1. **Feature flags**: Toggle new/old behavior
2. **Database backups**: Full backup before migration
3. **Staged rollout**: Dev → Staging → Production
4. **Comprehensive testing**: Automated + manual
5. **Rollback plan**: Revert scripts ready

---

## Success Criteria

- [ ] Zero compilation errors
- [ ] All tests passing
- [ ] No orgId in API URLs
- [ ] No x-org-id headers
- [ ] Database columns dropped
- [ ] Application runs in production
- [ ] No data loss
- [ ] Performance maintained

---

## Next Steps

1. **Review this plan** with team
2. **Choose execution strategy**
3. **Set timeline** and milestones
4. **Create backup strategy**
5. **Start with Phase 1**

---

**Document Version**: 1.0  
**Created**: October 28, 2025  
**Status**: Ready for execution
