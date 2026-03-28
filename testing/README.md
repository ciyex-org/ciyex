# Ciyex API — Permission Testing

Comprehensive SMART on FHIR role/permission tests for the Ciyex EHR API.

## Directory structure

```
testing/
├── README.md                     # This file
├── environments/
│   ├── local.env                 # Local dev environment variables
│   └── dev.env                   # Deployed dev environment variables
├── scripts/
│   ├── get-token.sh              # Obtain a JWT for any role
│   └── incremental-test.sh       # Automated incremental permission tests (curl)
└── http/
    ├── 00_get_tokens.http        # IDE HTTP client: fetch tokens for all roles
    ├── 01_no_auth.http           # Phase 1: all requests without auth → 401
    ├── 02_incremental_read.http  # Phase 3: add permissions incrementally
    └── 03_role_matrix.http       # Phase 4: full role × endpoint matrix
```

---

## JUnit tests (automated, no server required)

The Spring `@WebMvcTest` tests live in:
```
src/test/java/org/ciyex/ehr/security/
├── unit/
│   ├── ClinicalRoleTest.java              # ClinicalRole enum
│   ├── RolePermissionRegistryTest.java    # Role → scope mapping
│   └── PermissionConstantsTest.java       # SMART scope string format
└── api/
    ├── BaseApiSecurityTest.java            # Base class + JWT helpers
    ├── NoAuthAndEmptyPermissionTest.java   # Phase 1 + Phase 2
    ├── IncrementalPermissionSecurityTest.java # Phase 3
    └── FullRoleMatrixSecurityTest.java     # Phase 4
```

Run unit tests (no server needed):
```bash
cd ciyex-api
./gradlew test --tests "org.ciyex.ehr.security.*"
```

Run only pure unit tests (fastest):
```bash
./gradlew test --tests "org.ciyex.ehr.security.unit.*"
```

---

## Shell-based incremental test (against running server)

```bash
# Against dev environment
bash testing/scripts/incremental-test.sh dev

# Against local environment
bash testing/scripts/incremental-test.sh local
```

The script runs 6 phases:
1. **No auth** → all endpoints return 401
2. **ADMIN** → all endpoints accessible (200/201)
3. **PROVIDER** → clinical endpoints 200, admin endpoints 403
4. **BILLING** → financial endpoints 200, clinical write 403
5. **FRONT_DESK** → patient + scheduling 200, clinical/admin write 403
6. **PATIENT** → all staff endpoints 403

---

## Get a token for manual testing

```bash
# source the script to export TOKEN and AUTH_HEADER
source testing/scripts/get-token.sh dev admin

# Then use:
curl -H "Authorization: $AUTH_HEADER" $BASE_URL/api/fhir-resource/patients

# Or for a specific role:
source testing/scripts/get-token.sh dev billing
curl -H "Authorization: $AUTH_HEADER" $BASE_URL/api/fhir-resource/claims
```

---

## IDE HTTP client (.http files)

Open in IntelliJ IDEA or VS Code (REST Client extension).

1. Set environment variables in your IDE:
   - `BASE_URL` = `https://api-dev.ciyex.org` (or `http://localhost:8080`)
   - `KEYCLOAK_URL` = `https://dev.aran.me`
   - `REALM` = `ciyex`
   - `CLIENT_ID` = `ciyex-app`

2. Run `00_get_tokens.http` — this stores tokens as `token_admin`, `token_provider`, etc.
3. Run any other `.http` file using those stored tokens.

---

## Permission matrix (quick reference)

| Endpoint                                | ADMIN | PROVIDER | NURSE | MA  | FRONT_DESK | BILLING | PATIENT |
|-----------------------------------------|-------|----------|-------|-----|------------|---------|---------|
| GET  /fhir-resource/{tab}               | ✓     | ✓        | ✓     | ✓   | ✓          | ✓       | ✗       |
| POST /fhir-resource/{tab}               | ✓     | ✓        | ✓     | ✗   | ✓          | ✗       | ✗       |
| GET  /fhir-resource/{tab}/patient/{id}  | ✓     | ✓        | ✓     | ✓   | ✓          | ✓       | ✗       |
| POST /fhir-resource/{tab}/patient/{id}  | ✓     | ✓        | ✓     | ✗   | ✓          | ✗       | ✗       |
| GET  /fhir-resource/claims              | ✓     | ✓ (read) | ✗     | ✗   | ✗          | ✓       | ✗       |
| POST /fhir-resource/claims              | ✓     | ✗        | ✗     | ✗   | ✗          | ✓       | ✗       |
| GET  /admin/roles                       | ✓     | ✗        | ✗     | ✗   | ✗          | ✗       | ✗       |
| GET  /admin/users                       | ✓     | ✗        | ✗     | ✗   | ✗          | ✗       | ✗       |
| POST /admin/users                       | ✓     | ✗        | ✗     | ✗   | ✗          | ✗       | ✗       |

> PATIENT role has only `patient/*` scopes — accessible only via the patient portal endpoints.

---

## SMART on FHIR scope hierarchy

Scopes granted per role (from `RolePermissionRegistry.java`):

| Role       | Scopes                                                              |
|------------|---------------------------------------------------------------------|
| SUPER_ADMIN| `system/*.*` (all access)                                          |
| ADMIN      | All `user/*` read + write scopes                                    |
| PROVIDER   | All clinical `user/*` read + write; billing read-only; no admin write |
| NURSE      | Clinical docs + observations + immunizations; no prescriptions write |
| MA         | Observations write (vitals); appointments write; no patient write  |
| FRONT_DESK | Patient + appointments + consent write; no clinical/billing write  |
| BILLING    | Claims + coverage read/write; clinical read-only; no admin         |
| PATIENT    | `patient/*` read-only (self-access via portal only)               |
