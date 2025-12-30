# FHIR Migration Status

## Overview
This document tracks the FHIR migration status for all service classes in the ciyex project.

---

## COMPLETED - Migrated to FHIR Only (No Local Repository)

| Service | FHIR Resource | Status |
|---------|---------------|--------|
| `AllergyIntoleranceService` | AllergyIntolerance | FHIR Only |
| `MedicalProblemService` | Condition | FHIR Only |
| `ImmunizationService` | Immunization | FHIR Only |
| `LabOrderService` | ServiceRequest | FHIR Only |
| `LabResultService` | DiagnosticReport | FHIR Only |
| `FamilyHistoryService` | FamilyMemberHistory | FHIR Only |
| `PastMedicalHistoryService` | Condition (category: problem-list-item) | FHIR Only |
| `SocialHistoryService` | Observation (category: social-history) | FHIR Only |
| `ChiefComplaintService` | Condition (category: encounter-diagnosis) | FHIR Only |
| `AssessmentService` | ClinicalImpression | FHIR Only |
| `VitalsService` | Observation (category: vital-signs) | FHIR Only |
| `PatientService` | Patient | FHIR Only |
| `EncounterService` | Encounter | FHIR Only |
| `CoverageService` | Coverage | FHIR Only |
| `AppointmentService` | Appointment | FHIR Only |
| `HistoryOfPresentIllnessService` | Condition | FHIR Only |
| `MedicationRequestService` | MedicationRequest | FHIR Only |
| `PhysicalExamService` | Observation (category: exam) | FHIR Only |
| `PlanService` | CarePlan | FHIR Only |
| `ProcedureService` | Procedure | FHIR Only |
| `ReviewOfSystemService` | Observation (category: survey) | FHIR Only |
| `ProviderNoteService` | DocumentReference | FHIR Only |

---

## HYBRID - Local Repository + Optional FHIR Sync

These services use **local database as primary storage** with **optional FHIR sync** via `ExternalStorageResolver`:

| Service | FHIR Resource | Storage Pattern |
|---------|---------------|-----------------|
| `MedicationsService` | Medication | Local DB Only (Portal service - retrieves from MedicationRequestService) |

---

## NOT FHIR CANDIDATES - Administrative/Non-Clinical Services

| Service | Purpose |
|---------|---------|
| `AdminTemplateService` | Admin templates management |
| `AssignedProviderService` | Provider assignments |
| `AutomaticCreditTransferService` | Billing automation |
| `CodeService` | Code management |
| `CodeTypeService` | Code type management |
| `CommunicationService` | Messaging |
| `CreditCardService` | Payment processing |
| `DateTimeFinalizedService` | Date/time utilities |
| `DocumentService` | Document management |
| `DocumentSettingsService` | Document settings |
| `EncounterBrowserService` | Encounter browsing UI |
| `EncounterFeeScheduleService` | Fee schedules |
| `EncounterSummaryService` | Encounter summaries |
| `FacilityService` | Facility management |
| `GlobalCodeService` | Global codes |
| `GlobalEncounterSaveService` | Encounter save operations |
| `GpsBillingCardService` | GPS billing |
| `HealthcareServiceService` | Healthcare services |
| `InsuranceCardUploadService` | Insurance card uploads |
| `InsuranceCompanyService` | Insurance companies |
| `InventoryService` | Inventory management |
| `InventorySettingsService` | Inventory settings |
| `InvoiceService` | Invoicing |
| `KeycloakAdminService` | Keycloak admin |
| `KeycloakAuthService` | Authentication |
| `KeycloakOrgService` | Organization management |
| `KeycloakTokenService` | Token management |
| `KeycloakUserService` | User management |
| `ListOptionService` | List options |
| `LocationService` | Location management |
| `MaintenanceService` | System maintenance |
| `MessageAttachmentService` | Message attachments |
| `OrderService` | Order management |
| `OrgConfigService` | Organization config |
| `PatientBillingService` | Patient billing |
| `PatientCodeListService` | Patient code lists |
| `PatientEducationAssignmentService` | Education assignments |
| `PatientEducationService` | Patient education |
| `PatientHistoryService` | Patient history aggregation |
| `PatientMedicalHistoryService` | Medical history aggregation |
| `PatientRelationshipService` | Patient relationships |
| `PracticeContextService` | Practice/tenant context |
| `PracticeService` | Practice management |
| `ProviderScheduleService` | Provider schedules |
| `ProviderService` | Provider management |
| `ProviderSignatureService` | Provider signatures |
| `RecallService` | Patient recalls |
| `ReferralPracticeService` | Referral practices |
| `ReferralProviderService` | Referral providers |
| `ScheduleService` | Scheduling |
| `ServiceService` | Service management |
| `SignoffService` | Sign-off workflows |
| `SlotService` | Appointment slots |
| `SupplierService` | Supplier management |
| `TemplateDocumentService` | Document templates |
| `TemplateService` | Template management |
| `TenantDataMergeService` | Tenant data merge |
| `TenantSchemaService` | Tenant schema management |

---

## Summary Statistics

| Category | Count |
|----------|-------|
| **Total Services** | 74 |
| **Migrated to FHIR Only** | 22 |
| **Hybrid (Portal Services)** | 1 |
| **Administrative (Not FHIR Candidates)** | 51 |

---

## Implementation Notes

### Migrated Services Pattern
All migrated services follow this pattern:
- Use `FhirClientService` for all CRUD operations
- Use `PracticeContextService` for tenant context via "X-Request-Tenant-Id" header
- In-memory `ConcurrentHashMap` cache for e-sign/print metadata
- PDF generation preserved using PDFBox
- DTO-to-FHIR and FHIR-to-DTO mapping methods

### FHIR Resource Mappings
| DTO Field | FHIR Element |
|-----------|--------------|
| `patientId` | `subject` Reference |
| `encounterId` | `encounter` Reference |
| `fhirId` / `externalId` | Resource ID |
| `eSigned`, `signedAt`, `signedBy` | In-memory cache (not in FHIR) |
| `printedAt` | In-memory cache (not in FHIR) |

---

*Last Updated: January 2025*
