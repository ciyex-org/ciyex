# Ciyex EHR

Open-source Electronic Health Records (EHR) platform built with Spring Boot and FHIR R4.

## Overview

Ciyex EHR is a multi-tenant, FHIR-native EHR system designed for outpatient clinics and medical practices. It provides a comprehensive set of clinical, administrative, and billing features through a RESTful API.

### Key Features

- **Clinical Documentation** - Encounters, assessments, vitals, physical exams, review of systems, HPI, chief complaints, and provider notes
- **Patient Management** - Demographics, medical history, family history, social history, allergies, immunizations, and medications
- **Scheduling** - Appointments, provider schedules, slots, and recall management
- **Billing & Claims** - Insurance, claims, invoices, patient deposits, fee schedules, and payment processing (GPS/Stripe)
- **Orders & Results** - Lab orders, lab results, medication requests, procedures, and referrals
- **Documents** - Document management with S3 storage, PDF generation, templates, and e-signatures
- **Telehealth** - Video calls via Jitsi, Twilio, Cloudflare, and Telnyx integrations
- **Patient Portal** - Self-service registration, login, appointments, and document access
- **AI Assist** - Azure OpenAI and OpenAI integrations for clinical documentation
- **Notifications** - Email (SMTP/SendGrid) and SMS (Twilio/Telnyx)
- **Multi-tenancy** - Per-organization configuration with Keycloak-based tenant isolation
- **FHIR R4** - Native FHIR data model using HAPI FHIR client

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 4.0.1 |
| Build | Gradle |
| Auth | Keycloak (OAuth2/OIDC) + JWT |
| Data | FHIR R4 (HAPI FHIR 8.2.1) |
| Secrets | HashiCorp Vault |
| Config | Spring Cloud Config |
| Storage | Amazon S3 |
| PDF | OpenPDF, PDFBox, OpenHTMLtoPDF |
| Container | Docker (Eclipse Temurin 21) |
| CI/CD | GitHub Actions |
| Deployment | ArgoCD + Kustomize |

## Project Structure

```
src/main/java/org/ciyex/ehr/
  config/          # Security, CORS, S3, Keycloak configuration
  controller/      # REST API controllers (80+)
  dto/             # Data transfer objects
  eligibility/     # Insurance eligibility verification
  enums/           # Enumerations
  exception/       # Custom exceptions
  fhir/            # FHIR client service
  interceptor/     # Request interceptors
  security/        # JWT filters, Keycloak converters
  service/         # Business logic
    ai/            # AI-assisted documentation
    notification/  # Email and SMS
    portal/        # Patient portal auth
    telehealth/    # Video call providers
  storage/         # FHIR storage abstraction
  util/            # Utilities
```

## Prerequisites

- Java 21
- Gradle 9+
- A running FHIR R4 server (e.g., HAPI FHIR)
- Keycloak instance for authentication
- HashiCorp Vault for secrets management

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/ciyex-org/ciyex.git
cd ciyex
```

### 2. Configure environment variables

The application requires the following environment variables:

| Variable | Description |
|----------|-------------|
| `VAULT_URI` | HashiCorp Vault server URL |
| `VAULT_TOKEN` | Vault authentication token |
| `CONFIG_URI` | Spring Cloud Config server URL |
| `CONFIG_USERNAME` | Config server username |
| `CONFIG_PASSWORD` | Config server password |

Application secrets (loaded from Vault):

| Secret Path | Description |
|-------------|-------------|
| `jwt.secret` | JWT signing key (Base64-encoded) |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | Keycloak realm URL |
| `keycloak.auth-server-url` | Keycloak server URL |
| `keycloak.realm` | Keycloak realm name |
| `keycloak.credentials.secret` | Keycloak client secret |
| `keycloak.admin.username` | Keycloak admin username |
| `keycloak.admin.password` | Keycloak admin password |

### 3. Build

```bash
./gradlew build -x test
```

### 4. Run

```bash
./gradlew bootRun
```

The API will be available at `http://localhost:8080`.

### 5. Docker

```bash
DOCKER_BUILDKIT=1 docker build -t ciyex .
docker run -p 8080:8080 \
  -e VAULT_URI=https://your-vault-server \
  -e VAULT_TOKEN=your-token \
  -e CONFIG_URI=https://your-config-server \
  -e CONFIG_USERNAME=user \
  -e CONFIG_PASSWORD=pass \
  ciyex
```

## API Endpoints

All endpoints are under `/api/` and require authentication unless noted.

### Public Endpoints
- `POST /api/auth/**` - Authentication
- `GET /api/public/**` - Public resources
- `POST /api/portal/auth/**` - Patient portal authentication

### Clinical
- `/api/encounters` - Encounter management
- `/api/vitals` - Vital signs
- `/api/assessments` - Clinical assessments
- `/api/medications` - Medication management
- `/api/lab-orders` - Laboratory orders
- `/api/lab-results` - Laboratory results
- `/api/procedures` - Procedures
- `/api/immunizations` - Immunizations
- `/api/allergies` - Allergy documentation

### Administrative
- `/api/patients` - Patient demographics
- `/api/providers` - Provider management
- `/api/appointments` - Scheduling
- `/api/facilities` - Facility management
- `/api/users` - User management

### Billing
- `/api/claims` - Claims management
- `/api/invoices` - Invoice management
- `/api/gps` - Payment gateway

### Portal
- `/api/portal/**` - Patient portal endpoints

## Authentication

The API uses a hybrid JWT authentication model:

1. **Keycloak (primary)** - OAuth2/OIDC with RS256 JWTs from Keycloak JWKS endpoint
2. **Local JWT (portal)** - HS256 JWTs for patient portal authentication

Role-based access:
- `ADMIN` - Full access
- `PROVIDER` - Clinical and administrative access
- `PATIENT` - Portal access only

## Deployment

CI/CD is handled via GitHub Actions with a tag-based promotion flow:

1. **Push to main** - Builds and pushes `<version>-alpha.<n>` image to registry
2. **Promote to RC** - Retags latest alpha as `<version>-rc`
3. **Promote to GA** - Retags RC as `<version>` (semver)

ArgoCD Image Updater watches the registry and auto-deploys:
- `*-alpha.*` tags to **dev**
- `*-rc` tags to **stage**
- Semver tags to **prod**

## License

This project is licensed under the [GNU Affero General Public License v3.0](LICENSE) (AGPL-3.0).

If you modify this software and make it available over a network, you must release your source code under the same license.
