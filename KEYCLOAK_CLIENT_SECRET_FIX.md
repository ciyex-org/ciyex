# Keycloak Client Secret Fix

## Issue Found

The Keycloak client `ciyex-app` was created successfully, but the client secret in Keycloak didn't match the one in `application.yml`.

### ❌ Original (Incorrect)
```yaml
# application.yml
keycloak:
  credentials:
    secret: LgDl38nUg6leUfB9DCG5LDglC75bxpOp
```

### ✅ Correct Client Secret
```yaml
# application.yml
keycloak:
  credentials:
    secret: 00ZcJ2v50yWKwAa7fFvnfEYNWRAtderv
```

## What Was Fixed

1. **Updated `application.yml`** with the correct client secret: `00ZcJ2v50yWKwAa7fFvnfEYNWRAtderv`
2. **Verified user login** with `alice@example.com` now works

## ✅ Verification Tests

### Test 1: Admin User Login (Working)
```bash
curl --request POST \
  --url https://aran-stg.zpoa.com/realms/master/protocol/openid-connect/token \
  --header 'content-type: application/x-www-form-urlencoded' \
  --data 'grant_type=password' \
  --data 'client_id=ciyex-app' \
  --data 'client_secret=00ZcJ2v50yWKwAa7fFvnfEYNWRAtderv' \
  --data 'username=aran-admin' \
  --data 'password=Kc@2024!Secure#Pass'
```
✅ **Status**: Working - Returns access_token and refresh_token

### Test 2: Alice User Login (Working)
```bash
curl --request POST \
  --url https://aran-stg.zpoa.com/realms/master/protocol/openid-connect/token \
  --header 'content-type: application/x-www-form-urlencoded' \
  --data 'grant_type=password' \
  --data 'client_id=ciyex-app' \
  --data 'client_secret=00ZcJ2v50yWKwAa7fFvnfEYNWRAtderv' \
  --data 'username=alice@example.com' \
  --data 'password=Password@123'
```
✅ **Status**: Working - Returns access_token and refresh_token

### Test 3: Client Credentials Grant (Working)
```bash
curl -X POST "https://aran-stg.zpoa.com/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=ciyex-app" \
  -d "client_secret=00ZcJ2v50yWKwAa7fFvnfEYNWRAtderv"
```
✅ **Status**: Working - Returns access_token

## Client Configuration Verified

```json
{
  "directAccessGrantsEnabled": true,
  "serviceAccountsEnabled": true,
  "publicClient": false,
  "secret": "00ZcJ2v50yWKwAa7fFvnfEYNWRAtderv"
}
```

## Users Verified in Keycloak

1. **aran-admin** - Admin user ✅ Enabled
2. **alice@example.com** - Test user ✅ Enabled

## Next Steps

### 1. Update Environment Variables (If Used)
If you're using environment variables in production, update:

```bash
export KEYCLOAK_CLIENT_SECRET=00ZcJ2v50yWKwAa7fFvnfEYNWRAtderv
```

### 2. Update Frontend Configuration
If your frontend has the client secret hardcoded (it shouldn't, as it's confidential):

**EHR UI** - `ciyex-ehr-ui/.env.local`:
```bash
# Note: Client secret should NOT be in frontend for security reasons
# Frontend should use backend proxy for authentication
NEXT_PUBLIC_KEYCLOAK_ENABLED=true
NEXT_PUBLIC_KEYCLOAK_URL=https://aran-stg.zpoa.com
NEXT_PUBLIC_KEYCLOAK_REALM=master
NEXT_PUBLIC_KEYCLOAK_CLIENT_ID=ciyex-app
```

### 3. Restart Backend
After updating `application.yml`, restart your backend:

```bash
./start-backend.sh
```

### 4. Test Frontend Authentication
1. Start EHR UI: `./start-ehr-ui.sh`
2. Navigate to http://localhost:3000
3. Click "Aran ID (Keycloak)" button
4. Login with:
   - Username: `alice@example.com`
   - Password: `Password@123`
5. Verify successful login

## Security Notes

### ⚠️ Important Security Considerations

1. **Client Secret is Confidential**
   - Never expose client secret in frontend code
   - Only use in backend or server-side code
   - Store in environment variables for production

2. **Updated Scripts**
   The following files should be updated with the new secret:
   - `scripts/setup-keycloak-client.sh` (if you want to recreate)
   - `scripts/keycloak-client-config.json` (documentation only)
   - Any deployment scripts or Docker Compose files

3. **Rotate Secrets Regularly**
   - In Keycloak Admin Console: Clients → ciyex-app → Credentials → Regenerate
   - Update `application.yml` and environment variables
   - Restart applications

## Troubleshooting

### Error: "Invalid client credentials"
✅ **Fixed**: Updated client secret in application.yml

### Error: "Invalid username or password"
- Verify user exists in Keycloak
- Check user is enabled
- Verify password is correct
- Check user is in the correct realm (master)

### Error: "Unauthorized client"
- Verify Direct Access Grants is enabled (✅ Already enabled)
- Verify client is not public (✅ Already confidential)
- Verify client secret matches (✅ Now fixed)

## Complete Configuration Summary

### Keycloak Server
```
URL: https://aran-stg.zpoa.com
Realm: master
```

### Client Details
```
Client ID: ciyex-app
Client UUID: 9b9ef567-746b-4789-b302-5817db21bfbe
Client Secret: 00ZcJ2v50yWKwAa7fFvnfEYNWRAtderv
Access Type: confidential
```

### Client Capabilities
```
✅ Standard Flow (Authorization Code)
✅ Direct Access Grants (Password Grant)
✅ Service Accounts (Client Credentials)
✅ Authorization Services
```

### Redirect URIs Configured
```
14 redirect URIs covering:
- EHR UI (port 3000)
- Portal UI (port 3001)
- Backend API (port 8080)
- Keycloak server
```

### Test Users Available
```
1. aran-admin / Kc@2024!Secure#Pass (Admin)
2. alice@example.com / Password@123 (Test User)
```

---

**Status**: ✅ Fixed and Verified
**Date**: October 28, 2025
**Issue**: Client secret mismatch
**Resolution**: Updated application.yml with correct secret from Keycloak
