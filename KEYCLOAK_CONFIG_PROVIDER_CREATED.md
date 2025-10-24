# KeycloakConfigProvider Created (as OrgIntegrationConfigProvider)

## Summary

Created a new `OrgIntegrationConfigProvider` that loads configuration from Keycloak group attributes instead of database tables. This is a drop-in replacement for the old database-based provider.

## File Created

✅ `/src/main/java/com/qiaben/ciyex/util/OrgIntegrationConfigProvider.java`

## How It Works

### Old Approach (Database):
```java
// Load config from org_config table
OrgConfig orgConfig = orgConfigRepository.findByOrgId(orgId);
String fhirUrl = orgConfig.getFhirServerUrl();
```

### New Approach (Keycloak):
```java
// Load config from Keycloak group attributes
OrgIntegrationConfigProvider configProvider;
FhirConfig config = configProvider.getFhirConfigForCurrentOrg();
String fhirUrl = config.getServerUrl();
```

## Methods Provided

### Storage Configuration:
- `getStorageTypeForCurrentOrg()` - Get storage type (fhir, local, etc.)
- `getStorageConfigForCurrentOrg()` - Get full storage config
- `getS3DocumentStorage(Long orgId)` - Get S3 configuration

### Integration Configurations:
- `getFhirConfigForCurrentOrg()` - FHIR server configuration
- `getTelehealthConfigForCurrentOrg()` - Telehealth (Twilio, Telnyx, Jitsi)
- `getAiConfigForCurrentOrg()` - AI configuration (Azure, OpenAI)
- `getGpsConfigForCurrentOrg()` - GPS/Maps configuration
- `getTwilioConfigForCurrentOrg()` - SMS/Twilio configuration

### Generic Methods:
- `get(Long orgId, IntegrationKey key)` - Get config by integration key
- `getForCurrentOrg(IntegrationKey key)` - Get config for current org

### Backward Compatibility:
- `getGpsForCurrentOrg()` - Alias for GPS config
- `getStripeForCurrentOrg()` - Get Stripe configuration

## Keycloak Group Attributes

Configuration is stored as Keycloak group attributes:

```
/Tenants/practice_1
  Attributes:
    # Storage
    - storage_type: "fhir"
    - storage_vendor: "fhir"
    - s3_bucket_name: "my-bucket"
    - s3_region: "us-east-1"
    - s3_access_key_id: "AKIAXXXX"
    - s3_secret_access_key: "secret"
    
    # FHIR
    - fhir_server_url: "https://fhir.example.com"
    - fhir_client_id: "client123"
    - fhir_client_secret: "secret123"
    - fhir_token_url: "https://fhir.example.com/token"
    
    # Telehealth
    - telehealth_vendor: "telnyx"
    - telnyx_api_key: "KEY_xxx"
    - telnyx_api_secret: "SECRET_xxx"
    - telnyx_app_id: "APP_xxx"
    
    # AI
    - ai_vendor: "azure"
    - azure_openai_endpoint: "https://xxx.openai.azure.com"
    - azure_openai_api_key: "key123"
    - azure_deployment_name: "gpt-4"
    
    # GPS
    - gps_vendor: "google"
    - google_maps_api_key: "AIzaXXXX"
    
    # SMS
    - twilio_account_sid: "ACxxxx"
    - twilio_auth_token: "token"
    - twilio_phone_number: "+15551234567"
```

## Remaining Issues

The implementation has compilation errors because:

1. **DTO classes missing setters** - Config DTOs (FhirConfig, StorageConfig, etc.) don't have setter methods
2. **IntegrationKey enum** - Missing STORAGE and SMS enum values
3. **Nested class mismatches** - StorageConfig.S3 vs OrgIntegrationConfigProvider.S3Config

## Solutions

### Option 1: Update DTO Classes
Add setter methods to all config DTO classes:
- FhirConfig
- StorageConfig
- TelehealthConfig
- AiConfig
- GpsConfig
- TwilioConfig

### Option 2: Simplify Implementation
Return Map<String, Object> instead of typed DTOs and let services extract what they need.

### Option 3: Remove External Integrations
Since you're simplifying the architecture, consider removing all external integration code (FHIR, S3, Telehealth, etc.) and just use local storage.

## Recommendation

Given the extensive refactoring needed, I recommend:

1. **Remove all external integration services** that depend on complex configuration
2. **Keep only local/database storage** for simplicity
3. **Use Keycloak for authentication/authorization only**
4. **Store minimal org config** in Keycloak (just org name, schema name)

This would eliminate ~50+ service files and dramatically simplify the codebase.

## Files Still Needing Updates

~30+ service files still reference OrgIntegrationConfigProvider:
- PatientService, LabOrderService, RecallService
- FhirClientProvider, S3ClientProvider
- All telehealth services
- All AI services
- SMS/notification services
- And many more...

Would you like me to:
1. Fix all the DTO classes to add setters?
2. Remove all external integration services?
3. Something else?
