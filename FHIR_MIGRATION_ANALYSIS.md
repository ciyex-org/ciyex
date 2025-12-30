# FHIR Migration Analysis - Administrative Services

This document provides best-practice FHIR resource mappings for all administrative/non-clinical services.

---

## ALREADY MIGRATED TO FHIR

These services are already using FHIR resources:

| Service | FHIR Resource | Status |
|---------|---------------|--------|
| `FacilityService` | **Location** | FHIR Only |
| `LocationService` | **Location** | FHIR Only |
| `ProviderService` | **Practitioner** | FHIR Only |
| `PracticeService` | **Organization** | FHIR Only |

---

## COMPLETE FHIR MAPPING STRATEGY

### Templates & Forms

| Legacy Service | Best Practice Strategy | Primary FHIR Resources | Implementation Note |
|----------------|------------------------|------------------------|---------------------|
| `AdminTemplateService` | FHIR Questionnaire | **Questionnaire** | Use Questionnaire for defining forms/templates and QuestionnaireResponse for storing the filled data |
| `TemplateService` | FHIR Questionnaire | **Questionnaire** | Templates for forms/inputs |
| `TemplateDocumentService` | FHIR Document / Questionnaire | **DocumentReference / Questionnaire** | Templates for generated docs |

### Terminology & Codes

| Legacy Service | Best Practice Strategy | Primary FHIR Resources | Implementation Note |
|----------------|------------------------|------------------------|---------------------|
| `CodeService` | FHIR Terminology Server | **CodeSystem** | Store custom practice codes (CPT, ICD, Internal) as standardized CodeSystems |
| `CodeTypeService` | FHIR ValueSets | **ValueSet** | Defines "groups" of codes (e.g., "All Valid Service Codes") |
| `GlobalCodeService` | Shared FHIR Terminology | **CodeSystem** | Use a "Global" partition or Shared CodeSystem for codes valid across all practices |
| `ListOptionService` | FHIR ValueSets | **ValueSet** | $expand operation on ValueSet populates UI dropdowns automatically |

### Financial & Billing

| Legacy Service | Best Practice Strategy | Primary FHIR Resources | Implementation Note |
|----------------|------------------------|------------------------|---------------------|
| `AutomaticCreditTransferService` | External Financial System + FHIR Record | **PaymentReconciliation / PaymentNotice** | Do not process money in FHIR. Process in Stripe/Processor, record the transaction in FHIR |
| `CreditCardService` | External Gateway (Stripe/Authorize.net) | **N/A (Infrastructure)** | FHIR does not store raw credit card numbers (PCI Compliance). Store token refs in Account or Coverage if needed |
| `EncounterFeeScheduleService` | FHIR Billing Definitions | **ChargeItemDefinition** | Defines prices and billing codes for services |
| `GpsBillingCardService` | FHIR Extension | **ChargeItem** | Store GPS coordinates as a standard FHIR Extension on the ChargeItem resource |
| `PatientBillingService` | FHIR Financial | **Account / Invoice** | An 'Account' tracks the patient's balance; 'Invoice' tracks bills sent |
| `ServiceService` | FHIR Activity Definitions | **ActivityDefinition / ChargeItemDefinition** | Defines what services (bills) are available to be performed |

### Documents & Attachments

| Legacy Service | Best Practice Strategy | Primary FHIR Resources | Implementation Note |
|----------------|------------------------|------------------------|---------------------|
| `InsuranceCardUploadService` | FHIR Binary Storage | **DocumentReference + Binary** | Store the image in 'Binary', link it to Patient via 'DocumentReference' |
| `MessageAttachmentService` | FHIR Document Storage | **DocumentReference + Binary** | Same strategy as Insurance Cards |
| `ProviderSignatureService` | FHIR Provenance | **Provenance + Binary** | Store the signature image in Binary; link to data via Provenance (signature) element |

### Inventory & Supply

| Legacy Service | Best Practice Strategy | Primary FHIR Resources | Implementation Note |
|----------------|------------------------|------------------------|---------------------|
| `InventoryService` | FHIR Supply Workflow | **SupplyDelivery / Device** | Use SupplyDelivery for tracking usage and Device for equipment tracking |
| `InventorySettingsService` | App Database (Portal Schema) | **N/A** | Configuration for inventory limits/alerts belongs in App DB |
| `OrderService` | FHIR Supply Workflow | **SupplyRequest** | Requests for non-clinical supplies |
| `SupplierService` | FHIR Directory | **Organization (type=supplier)** | Suppliers are just Organizations with a specific 'type' |

### Scheduling

| Legacy Service | Best Practice Strategy | Primary FHIR Resources | Implementation Note |
|----------------|------------------------|------------------------|---------------------|
| `ProviderScheduleService` | FHIR Scheduling | **Schedule / Slot / Appointment** | Standard FHIR resources for calendar management |

### Clinical Workflow

| Legacy Service | Best Practice Strategy | Primary FHIR Resources | Implementation Note |
|----------------|------------------------|------------------------|---------------------|
| `PatientCodeListService` | FHIR Flags or Lists | **Flag / List** | Use 'Flag' for warnings (e.g., "Violent") or 'List' for arbitrary groupings |
| `PatientEducationAssignmentService` | FHIR Clinical Workflow | **ServiceRequest / Procedure** | Assigning education is a 'Request' for a service (education) |
| `SignoffService` | FHIR Workflow / Provenance | **Task / Provenance** | Use 'Task' to track the "Need to Sign" status; 'Provenance' to record the actual signature |

### Clinical Documents & Summaries

| Legacy Service | Best Practice Strategy | Primary FHIR Resources | Implementation Note |
|----------------|------------------------|------------------------|---------------------|
| `EncounterSummaryService` | FHIR Clinical Document | **Composition** | A summary of an encounter is technically a 'Composition' resource (like a mini-CDA) |
| `PatientMedicalHistoryService` | FHIR Clinical Document | **Composition** | A snapshot of medical history is a Composition |

### Search & Aggregation

| Legacy Service | Best Practice Strategy | Primary FHIR Resources | Implementation Note |
|----------------|------------------------|------------------------|---------------------|
| `EncounterBrowserService` | BFF (Backend for Frontend) | **Bundle (Search)** | This is a UI Aggregator. Use FHIR Search (`/Encounter?_include=Patient...`) to fetch data efficiently |
| `GlobalEncounterSaveService` | FHIR Transaction Bundle | **Bundle (transaction)** | Use a FHIR "Transaction Bundle" to save Encounter, Patient, and Conditions in one atomic HTTP POST |
| `PatientHistoryService` | FHIR Search / Bundle | **Bundle** | Do not store 'history' as a blob. Query it live using FHIR AuditEvent or Provenance |

### Organization & Configuration

| Legacy Service | Best Practice Strategy | Primary FHIR Resources | Implementation Note |
|----------------|------------------------|------------------------|---------------------|
| `OrgConfigService` | FHIR Organization or App DB | **Organization** | Store clinical configs (NPI, Taxonomy) in Organization; UI configs in App DB |

### Identity & Authentication (Keycloak)

| Legacy Service | Best Practice Strategy | Primary FHIR Resources | Implementation Note |
|----------------|------------------------|------------------------|---------------------|
| `KeycloakAdminService` | Identity Provider (IAM) | **N/A (Infrastructure)** | Managed by Keycloak |
| `KeycloakAuthService` | Identity Provider (IAM) | **N/A (Infrastructure)** | Managed by Keycloak (OIDC/OAuth2) |
| `KeycloakOrgService` | Identity Provider (IAM) | **Organization (Mirror)** | Keycloak handles Auth; FHIR 'Organization' handles Clinical data. Sync if necessary |
| `KeycloakTokenService` | Identity Provider (IAM) | **N/A (Infrastructure)** | Managed by Keycloak |
| `KeycloakUserService` | Identity Provider (IAM) | **Practitioner (Mirror)** | Keycloak handles Login; FHIR 'Practitioner' handles Clinical identity |

### Infrastructure & System

| Legacy Service | Best Practice Strategy | Primary FHIR Resources | Implementation Note |
|----------------|------------------------|------------------------|---------------------|
| `DateTimeFinalizedService` | Application Utility | **N/A (Java Library)** | This is logic, not data. Handle in Java/Go Util libraries |
| `DocumentSettingsService` | App Database (Portal Schema) | **Basic (or App DB)** | Storage settings belong in your "Portal" Postgres schema, not in clinical FHIR data |
| `MaintenanceService` | DevOps / Infrastructure | **N/A** | Handle via Kubernetes Jobs or Scripts |
| `PracticeContextService` | Architecture (Partitioning) | **N/A (Middleware)** | Handled by HAPI FHIR Partitioning + Java ThreadLocal Interceptors |
| `TenantDataMergeService` | Database Admin Tool | **N/A** | Infrastructure script (SQL) |
| `TenantSchemaService` | Database Config | **N/A** | Handled by Postgres RLS / Partitioning strategy |

---

## Summary by Category

| Category | Strategy | Count |
|----------|----------|-------|
| **FHIR Resources** | Direct FHIR mapping | 32 |
| **Infrastructure** | Keycloak/DevOps/DB | 10 |
| **App Database** | Portal schema config | 3 |

---

## Migration Priority

### Phase 1: Terminology & Forms
- `CodeService` → CodeSystem
- `CodeTypeService` → ValueSet
- `ListOptionService` → ValueSet
- `AdminTemplateService` → Questionnaire
- `TemplateService` → Questionnaire

### Phase 2: Documents & Attachments
- `InsuranceCardUploadService` → DocumentReference + Binary
- `MessageAttachmentService` → DocumentReference + Binary
- `ProviderSignatureService` → Provenance + Binary

### Phase 3: Financial
- `EncounterFeeScheduleService` → ChargeItemDefinition
- `ServiceService` → ActivityDefinition / ChargeItemDefinition
- `PatientBillingService` → Account / Invoice
- `AutomaticCreditTransferService` → PaymentReconciliation

### Phase 4: Inventory & Supply
- `InventoryService` → SupplyDelivery / Device
- `OrderService` → SupplyRequest
- `SupplierService` → Organization (type=supplier)

### Phase 5: Clinical Workflow
- `SignoffService` → Task / Provenance
- `PatientCodeListService` → Flag / List
- `PatientEducationAssignmentService` → ServiceRequest

### Phase 6: Summaries & Aggregation
- `EncounterSummaryService` → Composition
- `PatientMedicalHistoryService` → Composition
- `GlobalEncounterSaveService` → Bundle (transaction)

---

*Generated: January 2025*
