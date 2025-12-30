# Ciyex FHIR Migration Design Document

## Overview

This document outlines the migration strategy for moving clinical data from the Ciyex intermediate PostgreSQL database to HAPI FHIR server. The goal is to leverage FHIR R4 as the single source of truth for clinical data while keeping business/operational data in the existing database.

## Architecture

### Current State
```
┌─────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  Ciyex UI   │────▶│  Ciyex Backend   │────▶│  PostgreSQL DB  │
└─────────────┘     └──────────────────┘     │  (All Data)     │
                                              └─────────────────┘
```

### Target State
```
┌─────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  Ciyex UI   │────▶│  Ciyex Backend   │────▶│  PostgreSQL DB  │
└─────────────┘     └──────────────────┘     │  (Business Data)│
                           │                  └─────────────────┘
                           │
                           ▼
                    ┌──────────────────┐
                    │   HAPI FHIR R4   │
                    │  (Clinical Data) │
                    │  Partitioned by  │
                    │    Practice      │
                    └──────────────────┘
```

## FHIR Server Configuration

- **URL**: `https://fhir.apps-dev.in.hinisoft.com/fhir` (private/intranet only)
- **Version**: HAPI FHIR v8.6.0-1
- **FHIR Version**: R4
- **Partitioning**: Enabled (multi-practice support)
- **Authentication**: Keycloak OAuth2 (master realm)

## Entity to FHIR Resource Mapping

### Phase 1: Foundation (Patient & Organization)

| Ciyex Entity | FHIR Resource | Priority | Notes |
|--------------|---------------|----------|-------|
| `Patient` | Patient | P0 | Core entity, all others reference this |
| `Practice` | Organization | P0 | Maps to FHIR partition |
| `Provider` | Practitioner | P0 | Referenced by encounters |
| `Facility` | Location | P0 | Physical locations |
| `Location` | Location | P0 | Rooms within facilities |

### Phase 2: Clinical Encounters

| Ciyex Entity | FHIR Resource | Priority | Notes |
|--------------|---------------|----------|-------|
| `Encounter` | Encounter | P1 | Clinical visits |
| `Appointment` | Appointment | P1 | Scheduled visits |
| `Vitals` | Observation (vital-signs) | P1 | BP, HR, Temp, etc. |
| `ChiefComplaint` | Encounter.reasonCode | P1 | Part of Encounter |
| `Assessment` | Condition (encounter-diagnosis) | P1 | Diagnoses |

### Phase 3: Clinical History

| Ciyex Entity | FHIR Resource | Priority | Notes |
|--------------|---------------|----------|-------|
| `AllergyIntolerance` | AllergyIntolerance | P2 | Patient allergies |
| `MedicalProblem` | Condition | P2 | Problem list |
| `MedicationRequest` | MedicationRequest | P2 | Prescriptions |
| `Immunization` | Immunization | P2 | Vaccination records |
| `FamilyHistory` | FamilyMemberHistory | P2 | Family medical history |
| `PastMedicalHistory` | Condition | P2 | Historical conditions |
| `SocialHistory` | Observation (social-history) | P2 | Smoking, alcohol, etc. |

### Phase 4: Diagnostics & Procedures

| Ciyex Entity | FHIR Resource | Priority | Notes |
|--------------|---------------|----------|-------|
| `LabOrder` | ServiceRequest | P3 | Lab orders |
| `LabResult` | DiagnosticReport + Observation | P3 | Lab results |
| `Procedure` | Procedure | P3 | Clinical procedures |
| `PhysicalExam` | Observation | P3 | Exam findings |

### Phase 5: Documents & Coverage

| Ciyex Entity | FHIR Resource | Priority | Notes |
|--------------|---------------|----------|-------|
| `Document` | DocumentReference | P4 | Clinical documents |
| `PatientDocument` | DocumentReference | P4 | Patient-specific docs |
| `Coverage` | Coverage | P4 | Insurance coverage |
| `InsuranceCompany` | Organization (ins) | P4 | Payers |

### Entities Remaining in PostgreSQL (Non-Clinical)

| Entity | Reason |
|--------|--------|
| `Invoice`, `InvoiceLine`, `InvoicePayment` | Billing/financial |
| `PatientClaim`, `PatientClaimDocument` | Claims processing |
| `Schedule`, `Slot` | Scheduling metadata |
| `AuditLog` | System audit trail |
| `AdminTemplate`, `Template` | UI templates |
| `OrgConfig`, `DocumentSettings` | Configuration |
| `Inventory`, `Supplier` | Inventory management |
| `Communication`, `Subscription` | Messaging |
| `Code`, `CodeType`, `GlobalCode` | Reference data |

---

## Phase 1: Foundation

### 1.1 FHIR Client Library

Create a reusable FHIR client service in Ciyex backend.

**New Classes:**
```
com.qiaben.ciyex.fhir/
├── FhirClientConfig.java          # FHIR client configuration
├── FhirClientService.java         # Generic FHIR operations
├── FhirPartitionInterceptor.java  # Practice-based partitioning
└── mapper/
    ├── PatientFhirMapper.java     # Patient ↔ FHIR Patient
    ├── PractitionerFhirMapper.java
    ├── OrganizationFhirMapper.java
    └── LocationFhirMapper.java
```

**Dependencies to Add (pom.xml):**
```xml
<dependency>
    <groupId>ca.uhn.hapi.fhir</groupId>
    <artifactId>hapi-fhir-client</artifactId>
    <version>8.0.0</version>
</dependency>
<dependency>
    <groupId>ca.uhn.hapi.fhir</groupId>
    <artifactId>hapi-fhir-structures-r4</artifactId>
    <version>8.0.0</version>
</dependency>
```

**Configuration (application.yml):**
```yaml
fhir:
  server:
    url: https://fhir.apps-dev.in.hinisoft.com/fhir
  client:
    socket-timeout: 60000
    connect-timeout: 10000
```

### 1.2 Patient Migration

**Strategy:** Dual-write during transition, then cutover.

**Migration Steps:**
1. Add FHIR client to PatientService
2. On Patient create/update → write to both DB and FHIR
3. On Patient read → read from FHIR (fallback to DB)
4. Migrate existing patients via batch job
5. Remove DB writes after validation

**FHIR Patient Resource Structure:**
```json
{
  "resourceType": "Patient",
  "id": "patient-uuid",
  "identifier": [
    {
      "system": "urn:ciyex:patient:mrn",
      "value": "MRN-12345"
    }
  ],
  "name": [{
    "use": "official",
    "family": "Smith",
    "given": ["John", "Michael"]
  }],
  "gender": "male",
  "birthDate": "1990-01-15",
  "address": [{
    "use": "home",
    "line": ["123 Main St"],
    "city": "Orlando",
    "state": "FL",
    "postalCode": "32801"
  }],
  "telecom": [
    {"system": "phone", "value": "555-123-4567", "use": "mobile"},
    {"system": "email", "value": "john.smith@email.com"}
  ],
  "managingOrganization": {
    "reference": "Organization/practice-500"
  }
}
```

### 1.3 Practice → Organization Migration

Each practice becomes a FHIR Organization and a partition.

**Partition Setup:**
```java
// Create partition for each practice
PartitionEntity partition = new PartitionEntity();
partition.setId(practiceId);
partition.setName("practice-" + practiceId);
```

---

## Phase 2: Clinical Encounters

### 2.1 Encounter Migration

**FHIR Encounter Resource:**
```json
{
  "resourceType": "Encounter",
  "id": "encounter-uuid",
  "status": "finished",
  "class": {
    "system": "http://terminology.hl7.org/CodeSystem/v3-ActCode",
    "code": "AMB",
    "display": "ambulatory"
  },
  "subject": {
    "reference": "Patient/patient-uuid"
  },
  "participant": [{
    "individual": {
      "reference": "Practitioner/provider-uuid"
    }
  }],
  "period": {
    "start": "2024-01-15T09:00:00Z",
    "end": "2024-01-15T09:30:00Z"
  },
  "reasonCode": [{
    "coding": [{
      "system": "http://snomed.info/sct",
      "code": "386661006",
      "display": "Fever"
    }]
  }],
  "location": [{
    "location": {
      "reference": "Location/facility-uuid"
    }
  }]
}
```

### 2.2 Vitals → Observation

**FHIR Observation (vital-signs):**
```json
{
  "resourceType": "Observation",
  "id": "vitals-uuid",
  "status": "final",
  "category": [{
    "coding": [{
      "system": "http://terminology.hl7.org/CodeSystem/observation-category",
      "code": "vital-signs"
    }]
  }],
  "code": {
    "coding": [{
      "system": "http://loinc.org",
      "code": "85354-9",
      "display": "Blood pressure panel"
    }]
  },
  "subject": {
    "reference": "Patient/patient-uuid"
  },
  "encounter": {
    "reference": "Encounter/encounter-uuid"
  },
  "effectiveDateTime": "2024-01-15T09:15:00Z",
  "component": [
    {
      "code": {
        "coding": [{
          "system": "http://loinc.org",
          "code": "8480-6",
          "display": "Systolic blood pressure"
        }]
      },
      "valueQuantity": {
        "value": 120,
        "unit": "mmHg",
        "system": "http://unitsofmeasure.org",
        "code": "mm[Hg]"
      }
    },
    {
      "code": {
        "coding": [{
          "system": "http://loinc.org",
          "code": "8462-4",
          "display": "Diastolic blood pressure"
        }]
      },
      "valueQuantity": {
        "value": 80,
        "unit": "mmHg",
        "system": "http://unitsofmeasure.org",
        "code": "mm[Hg]"
      }
    }
  ]
}
```

---

## Phase 3: Clinical History

### 3.1 AllergyIntolerance
### 3.2 MedicalProblem → Condition
### 3.3 MedicationRequest
### 3.4 Immunization
### 3.5 FamilyHistory → FamilyMemberHistory

---

## Phase 4: Diagnostics & Procedures

### 4.1 LabOrder → ServiceRequest
### 4.2 LabResult → DiagnosticReport + Observation
### 4.3 Procedure

---

## Phase 5: Documents & Coverage

### 5.1 Document → DocumentReference
### 5.2 Coverage

---

## Migration Strategy

### Dual-Write Pattern

During migration, write to both systems:

```java
@Transactional
public Patient createPatient(PatientDto dto) {
    // 1. Write to PostgreSQL (existing)
    Patient patient = patientRepository.save(mapToEntity(dto));
    
    // 2. Write to FHIR
    org.hl7.fhir.r4.model.Patient fhirPatient = patientFhirMapper.toFhir(patient);
    fhirClientService.create(fhirPatient, patient.getPracticeId());
    
    return patient;
}
```

### Read Strategy

Read from FHIR first, fallback to DB:

```java
public PatientDto getPatient(UUID id, String practiceId) {
    try {
        // Try FHIR first
        org.hl7.fhir.r4.model.Patient fhirPatient = 
            fhirClientService.read(Patient.class, id.toString(), practiceId);
        return patientFhirMapper.toDto(fhirPatient);
    } catch (ResourceNotFoundException e) {
        // Fallback to DB
        return patientRepository.findById(id)
            .map(this::mapToDto)
            .orElseThrow();
    }
}
```

### Batch Migration

For existing data:

```java
@Scheduled(cron = "0 0 2 * * *") // 2 AM daily
public void migratePatientsBatch() {
    List<Patient> unmigrated = patientRepository.findByFhirIdIsNull();
    for (Patient patient : unmigrated) {
        try {
            org.hl7.fhir.r4.model.Patient fhirPatient = 
                patientFhirMapper.toFhir(patient);
            MethodOutcome outcome = fhirClientService.create(fhirPatient);
            patient.setFhirId(outcome.getId().getIdPart());
            patientRepository.save(patient);
        } catch (Exception e) {
            log.error("Failed to migrate patient {}", patient.getId(), e);
        }
    }
}
```

---

## Timeline

| Phase | Duration | Deliverables |
|-------|----------|--------------|
| Phase 1 | 2 weeks | FHIR client, Patient/Organization/Practitioner/Location |
| Phase 2 | 2 weeks | Encounter, Appointment, Vitals, Assessment |
| Phase 3 | 2 weeks | Allergies, Problems, Medications, Immunizations, Family History |
| Phase 4 | 2 weeks | Lab Orders/Results, Procedures |
| Phase 5 | 1 week | Documents, Coverage |
| Cleanup | 1 week | Remove deprecated DB tables, final testing |

**Total: ~10 weeks**

---

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Data loss during migration | Dual-write pattern, keep DB as backup |
| Performance degradation | FHIR caching, batch operations |
| FHIR server downtime | Circuit breaker, fallback to DB |
| Complex queries | Use FHIR search parameters, denormalize if needed |

---

## Success Criteria

1. All clinical data accessible via FHIR API
2. No data loss during migration
3. Response times < 500ms for single resource reads
4. Multi-practice isolation via partitioning
5. Audit trail maintained

---

## Next Steps

1. [ ] Add HAPI FHIR client dependencies to pom.xml
2. [ ] Create FhirClientConfig and FhirClientService
3. [ ] Create PatientFhirMapper
4. [ ] Modify PatientService for dual-write
5. [ ] Create batch migration job
6. [ ] Test with practice-500 data
