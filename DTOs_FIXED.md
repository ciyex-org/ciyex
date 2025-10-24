# DTOs Fixed - Configuration Loading from Keycloak

## Summary

Fixed all configuration DTOs to properly load from Keycloak tenant group attributes instead of parsing JSON. All DTOs now use Lombok `@Data` annotation which provides getters/setters automatically.

## What Was Fixed

### ✅ OrgIntegrationConfigProvider.java

Updated all configuration loading methods to:
1. Load attributes directly from Keycloak (no JSON parsing)
2. Map to correct DTO field names
3. Handle nested configuration objects properly

### Configuration Methods Updated:

#### 1. FhirConfig ✅
```java
// Keycloak Attributes:
- fhir_api_url
- fhir_client_id
- fhir_client_secret
- fhir_token_url
- fhir_scope

// Maps to FhirConfig fields:
- apiUrl
- clientId
- clientSecret
- tokenUrl
- scope
```

#### 2. StorageConfig ✅
```java
// Keycloak Attributes:
- s3_bucket
- s3_region
- s3_access_key
- s3_secret_key

// Maps to StorageConfig.S3 fields:
- bucket
- region
- accessKey
- secretKey
```

#### 3. TelehealthConfig ✅
```java
// Keycloak Attributes:
- telehealth_vendor (telnyx|twilio|jitsi)

// Telnyx:
- telnyx_api_key
- telnyx_from_number

// Twilio:
- twilio_account_sid
- twilio_auth_token
- twilio_api_key_sid
- twilio_api_key_secret
- twilio_messaging_service_sid

// Jitsi:
- jitsi_server_url
- jitsi_app_id
- jitsi_app_secret
- jitsi_enable_recording (boolean)
- jitsi_default_token_ttl (integer)
```

#### 4. AiConfig ✅
```java
// Keycloak Attributes:
- ai_vendor (azure|openai|mock)

// Azure:
- azure_endpoint
- azure_api_key
- azure_deployment
- azure_api_version
- azure_use_managed_identity (boolean)
- azure_timeout_ms (integer)

// OpenAI:
- openai_api_key
- openai_model
- openai_endpoint

// Mock:
- mock_fixed_response

// Defaults:
- ai_default_temperature (double)
- ai_default_max_tokens (integer)
- ai_default_top_p (double)
```

#### 5. GpsConfig ✅
```java
// Keycloak Attributes (GPS Payment Gateway):
- gps_username
- gps_password
- gps_security_key
- gps_collectjs_public_key
- gps_transact_url
- gps_webhook_url
```

#### 6. TwilioConfig ✅
```java
// Keycloak Attributes:
- twilio_account_sid
- twilio_auth_token
- twilio_phone_number
```

### Helper Methods Added:

#### Generic Configuration Access:
```java
// Get config by integration key
<T> T get(Long orgId, IntegrationKey key)
<T> T getForCurrentTenant(IntegrationKey key)

// Backward compatibility
GpsConfig getGpsForCurrentOrg()
Map<String, Object> getStripeForCurrentOrg()
S3Config getS3DocumentStorage(Long orgId)
```

### S3Config Inner Class:
```java
public static class S3Config {
    private String bucketName;
    private String region;
    private String accessKeyId;
    private String secretAccessKey;
    
    // Backward compatibility aliases
    public String getBucket() { return bucketName; }
    public String getAccessKey() { return accessKeyId; }
    public String getSecretKey() { return secretAccessKey; }
}
```

## How It Works

### Before (JSON Parsing):
```java
// Old approach - parse JSON from database
String configJson = orgConfig.getFhirConfig();
FhirConfig config = objectMapper.readValue(configJson, FhirConfig.class);
```

### After (Direct Attribute Loading):
```java
// New approach - load directly from Keycloak
Map<String, Object> attributes = keycloakOrgService.getAllAttributes(tenantGroup);

FhirConfig config = new FhirConfig();
config.setApiUrl((String) attributes.get("fhir_api_url"));
config.setClientId((String) attributes.get("fhir_client_id"));
// ... etc
```

## Keycloak Group Structure

### Example Configuration:
```
/Tenants/practice_1
  Attributes:
    # FHIR
    fhir_api_url: "https://fhir.example.com/r4"
    fhir_client_id: "client123"
    fhir_client_secret: "secret123"
    fhir_token_url: "https://fhir.example.com/oauth/token"
    fhir_scope: "system/*.read system/*.write"
    
    # Storage (S3)
    s3_bucket: "practice-1-documents"
    s3_region: "us-east-1"
    s3_access_key: "AKIAXXXX"
    s3_secret_key: "secretXXXX"
    
    # Telehealth (Jitsi)
    telehealth_vendor: "jitsi"
    jitsi_server_url: "https://meet.example.com"
    jitsi_app_id: "app123"
    jitsi_app_secret: "secret123"
    jitsi_enable_recording: "true"
    jitsi_default_token_ttl: "3600"
    
    # AI (Azure)
    ai_vendor: "azure"
    azure_endpoint: "https://xxx.openai.azure.com"
    azure_api_key: "key123"
    azure_deployment: "gpt-4"
    azure_api_version: "2024-02-15-preview"
    azure_use_managed_identity: "false"
    azure_timeout_ms: "30000"
    ai_default_temperature: "0.7"
    ai_default_max_tokens: "2000"
    ai_default_top_p: "1.0"
    
    # GPS Payment Gateway
    gps_username: "merchant123"
    gps_password: "password123"
    gps_security_key: "key123"
    gps_collectjs_public_key: "pk_xxx"
    gps_transact_url: "https://api.gps.com/transact"
    gps_webhook_url: "https://myapp.com/webhooks/gps"
    
    # SMS (Twilio)
    twilio_account_sid: "ACxxxx"
    twilio_auth_token: "token123"
    twilio_phone_number: "+15551234567"
```

## Remaining Issues

### Compilation Errors: ~154 errors

Main categories:
1. **JwtTokenUtil** - References removed scope classes (needs removal)
2. **Portal services** - Reference removed User/Auth classes
3. **Payment services** - Reference removed Stripe service
4. **User controllers** - Reference removed User service
5. **IntegrationKey enum** - Missing STORAGE and SMS values

### Files That Need Removal:
- JwtTokenUtil.java
- PortalApprovalService.java
- MultiTenantAuthService.java
- PaymentOrderService.java
- UserController.java
- DocumentSettingsController.java
- PortalAuthController.java

### IntegrationKey Enum Needs Update:
```java
public enum IntegrationKey {
    FHIR,
    STORAGE,  // ADD THIS
    TELEHEALTH,
    AI,
    GPS,
    SMS       // ADD THIS
}
```

## Benefits

### ✅ No JSON Parsing
- Direct attribute access
- No ObjectMapper dependency
- Simpler code

### ✅ Type Safety
- Proper DTOs with Lombok
- Compile-time checking
- IDE autocomplete

### ✅ Flexibility
- Easy to add new attributes
- No schema changes needed
- Update in Keycloak only

### ✅ Single Source of Truth
- All config in Keycloak
- No database sync issues
- Centralized management

## Next Steps

1. **Remove problematic files** (~7 files)
2. **Fix IntegrationKey enum** (add STORAGE, SMS)
3. **Test compilation** (should reduce errors significantly)
4. **Test runtime** (verify Keycloak integration)
5. **Document attribute names** (for ops team)

## Testing

### Unit Test Example:
```java
@Test
void testFhirConfigLoading() {
    // Mock Keycloak attributes
    Map<String, Object> attributes = Map.of(
        "fhir_api_url", "https://fhir.test.com",
        "fhir_client_id", "test-client"
    );
    
    // Load config
    FhirConfig config = configProvider.getFhirConfigForCurrentOrg();
    
    // Verify
    assertEquals("https://fhir.test.com", config.getApiUrl());
    assertEquals("test-client", config.getClientId());
}
```

### Integration Test:
1. Create test tenant group in Keycloak
2. Add test attributes
3. Call config provider
4. Verify correct values loaded

## Documentation for Ops

### Adding New Configuration:
1. Go to Keycloak Admin Console
2. Navigate to Groups → /Tenants/practice_X
3. Click "Attributes" tab
4. Add key-value pairs
5. Save
6. Application will load on next request (no restart needed)

### Updating Configuration:
1. Edit attribute value in Keycloak
2. Save
3. Changes take effect immediately

### Removing Configuration:
1. Delete attribute from Keycloak
2. Application will return null for that config

---

**Status**: DTOs Fixed ✅  
**Compilation**: Still has errors (~154)  
**Next**: Remove problematic files and fix IntegrationKey enum
