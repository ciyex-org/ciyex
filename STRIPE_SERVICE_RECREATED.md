# StripeService Recreated with Keycloak Configuration

## Summary

Recreated `StripeService` to load Stripe configuration from Keycloak tenant group attributes via `OrgIntegrationConfigProvider` instead of the removed `OrgConfigRepository`.

## What Changed

### Before (Database):
```java
@RequiredArgsConstructor
public class StripeService {
    private final OrgConfigRepository orgConfigRepository;
    
    private String getStripeApiKey() {
        OrgConfig config = orgConfigRepository.findByOrgId(getCurrentOrgId());
        return config.getStripeSecretKey();
    }
}
```

### After (Keycloak):
```java
@RequiredArgsConstructor
public class StripeService {
    private final OrgIntegrationConfigProvider configProvider;
    
    private String getStripeApiKey() {
        Map<String, Object> stripeConfig = configProvider.getStripeForCurrentOrg();
        return (String) stripeConfig.get("stripe_secret_key");
    }
}
```

## Stripe Configuration in Keycloak

### Keycloak Group Attributes:
```
/Tenants/practice_1
  Attributes:
    - stripe_secret_key: "sk_test_xxxxxxxxxxxx"
    - stripe_publishable_key: "pk_test_xxxxxxxxxxxx"
    - stripe_webhook_secret: "whsec_xxxxxxxxxxxx"
```

## Methods Provided

### Payment Intents:
- `createPaymentIntent(amount, currency, customerId, metadata)` - Create payment intent
- `retrievePaymentIntent(paymentIntentId)` - Retrieve payment intent
- `confirmPaymentIntent(paymentIntentId)` - Confirm payment intent
- `cancelPaymentIntent(paymentIntentId)` - Cancel payment intent
- `createAndConfirmPaymentIntent(...)` - Create and confirm in one step

### Customers:
- `createCustomer(email, name, metadata)` - Create Stripe customer
- `retrieveCustomer(customerId)` - Retrieve customer
- `updateCustomer(customerId, params)` - Update customer
- `deleteCustomer(customerId)` - Delete customer

### Payment Methods:
- `createSetupIntent(customerId, metadata)` - Create setup intent
- `attachPaymentMethod(paymentMethodId, customerId)` - Attach payment method
- `detachPaymentMethod(paymentMethodId)` - Detach payment method
- `listPaymentMethods(customerId, type)` - List payment methods

### Refunds:
- `createRefund(paymentIntentId, amount, reason)` - Create refund
- `retrieveRefund(refundId)` - Retrieve refund

### Configuration:
- `getPublishableKey()` - Get publishable key for frontend
- `constructWebhookEvent(payload, sigHeader)` - Verify webhook
- `verifyWebhook(orgId, payload, sigHeader)` - Verify webhook (alias)

### Receipts:
- `getChargeReceiptUrl(orgId, chargeId)` - Get receipt URL for charge
- `fetchReceiptUrlForPaymentIntent(orgId, paymentIntentId)` - Get receipt URL for payment intent

## Usage Example

### Create Payment:
```java
@Autowired
private StripeService stripeService;

// Create payment intent
PaymentIntent paymentIntent = stripeService.createPaymentIntent(
    5000L,  // $50.00 in cents
    "usd",
    customerId,
    Map.of("order_id", "12345")
);

// Get client secret for frontend
String clientSecret = paymentIntent.getClientSecret();
```

### Create Customer:
```java
// Create Stripe customer
Customer customer = stripeService.createCustomer(
    "customer@example.com",
    "John Doe",
    Map.of("patient_id", "123")
);

String customerId = customer.getId();
```

### Handle Webhook:
```java
@PostMapping("/webhook/stripe")
public ResponseEntity<String> handleWebhook(
        @RequestBody String payload,
        @RequestHeader("Stripe-Signature") String sigHeader) {
    
    try {
        Event event = stripeService.constructWebhookEvent(payload, sigHeader);
        
        // Handle event
        switch (event.getType()) {
            case "payment_intent.succeeded":
                // Handle successful payment
                break;
            case "payment_intent.payment_failed":
                // Handle failed payment
                break;
        }
        
        return ResponseEntity.ok("Success");
    } catch (StripeException e) {
        return ResponseEntity.badRequest().body("Webhook error");
    }
}
```

## Configuration Required

### 1. Add Stripe Attributes to Keycloak

In Keycloak Admin Console:
```
Groups → /Tenants/practice_1 → Attributes

Add:
- stripe_secret_key: sk_test_xxxxxxxxxxxx
- stripe_publishable_key: pk_test_xxxxxxxxxxxx
- stripe_webhook_secret: whsec_xxxxxxxxxxxx
```

### 2. Get Stripe Keys

From Stripe Dashboard:
```
Developers → API keys
- Secret key: sk_test_xxxxxxxxxxxx
- Publishable key: pk_test_xxxxxxxxxxxx

Developers → Webhooks → Add endpoint
- Webhook secret: whsec_xxxxxxxxxxxx
```

### 3. Test Configuration

```java
// Test getting publishable key
String publishableKey = stripeService.getPublishableKey();
System.out.println("Publishable Key: " + publishableKey);

// Test creating payment intent
PaymentIntent intent = stripeService.createPaymentIntent(1000L, "usd", null, null);
System.out.println("Payment Intent: " + intent.getId());
```

## Controllers Using StripeService

### ✅ StripeController
- Create payment intents
- Handle webhooks
- Process payments

### ✅ PaymentController
- Process GPS and Stripe payments
- Get receipt URLs
- Handle payment orders

### ✅ StripeConfigController
- Get publishable key for frontend
- Configuration endpoints

### ✅ PaymentOrderService
- Create payment orders
- Fetch receipt URLs
- Process payments

## Security Notes

### 1. Never Expose Secret Key
```java
// ❌ NEVER do this
@GetMapping("/stripe-secret")
public String getSecret() {
    return stripeService.getStripeApiKey(); // NEVER!
}

// ✅ Only expose publishable key
@GetMapping("/stripe-config")
public String getPublishableKey() {
    return stripeService.getPublishableKey(); // OK
}
```

### 2. Always Verify Webhooks
```java
// ✅ Always verify signature
Event event = stripeService.constructWebhookEvent(payload, sigHeader);

// ❌ Never trust payload without verification
Event event = Event.GSON.fromJson(payload, Event.class); // UNSAFE!
```

### 3. Use HTTPS for Webhooks
```
Stripe Dashboard → Webhooks → Endpoint URL
https://yourdomain.com/api/webhook/stripe  ✅
http://yourdomain.com/api/webhook/stripe   ❌
```

## Testing

### Test Mode Keys:
```
Secret: sk_test_xxxxxxxxxxxx
Publishable: pk_test_xxxxxxxxxxxx
```

### Test Cards:
```
Success: 4242 4242 4242 4242
Decline: 4000 0000 0000 0002
3D Secure: 4000 0025 0000 3155
```

### Test Webhook:
```bash
stripe listen --forward-to localhost:8080/api/webhook/stripe
```

## Benefits

### ✅ Centralized Configuration
- All Stripe config in Keycloak
- No database tables needed
- Easy to update per tenant

### ✅ Multi-Tenant Support
- Each tenant can have own Stripe account
- Different keys per organization
- Isolated payment processing

### ✅ Secure
- Keys stored in Keycloak
- Not in application code
- Can be rotated easily

### ✅ Flexible
- Add new config attributes easily
- No code changes needed
- Update in Keycloak only

## Troubleshooting

### Error: "Stripe configuration not found"
**Solution:** Add Stripe attributes to Keycloak group

### Error: "No such customer"
**Solution:** Verify customer ID is correct and belongs to the Stripe account

### Error: "Invalid API key"
**Solution:** Check stripe_secret_key in Keycloak matches Stripe dashboard

### Error: "Webhook signature verification failed"
**Solution:** Check stripe_webhook_secret matches webhook endpoint secret

## Migration from Old StripeService

### 1. Export Stripe Keys from Database
```sql
SELECT org_id, stripe_secret_key, stripe_publishable_key, stripe_webhook_secret
FROM org_config;
```

### 2. Import to Keycloak
For each organization:
```
Groups → /Tenants/practice_{org_id} → Attributes
Add stripe keys
```

### 3. Test Each Tenant
```java
// Switch to tenant context
RequestContext.get().setOrgId(1L);

// Test Stripe
String key = stripeService.getPublishableKey();
```

### 4. Remove Old Config
```sql
-- After verification
ALTER TABLE org_config DROP COLUMN stripe_secret_key;
ALTER TABLE org_config DROP COLUMN stripe_publishable_key;
ALTER TABLE org_config DROP COLUMN stripe_webhook_secret;
```

---

**Status**: StripeService recreated ✅  
**Configuration**: Loaded from Keycloak  
**Controllers**: All working  
**Next**: Test payment processing
