# Keycloak Client Setup Verification Checklist

Use this checklist to ensure your Keycloak client is properly configured.

## 🔐 Admin Access

- [ ] Can access Keycloak Admin Console at https://aran-stg.zpoa.com
- [ ] Can login with username: `aran-admin`
- [ ] Can see `master` realm in dropdown

## 📝 Client Configuration

### Basic Settings
- [ ] Client `ciyex-app` exists in `master` realm
- [ ] Client is **Enabled**
- [ ] **Client Protocol** is `openid-connect`
- [ ] **Access Type** is `confidential`

### Flow Settings
- [ ] **Standard Flow Enabled**: ON
- [ ] **Implicit Flow Enabled**: OFF
- [ ] **Direct Access Grants Enabled**: ON
- [ ] **Service Accounts Enabled**: ON
- [ ] **Authorization Enabled**: ON

### Redirect URIs (14 total)
- [ ] `http://localhost:3000/*`
- [ ] `http://localhost:3000/api/auth/callback`
- [ ] `http://localhost:3000/signin`
- [ ] `http://localhost:3000/dashboard`
- [ ] `http://localhost:3000/select-practice`
- [ ] `http://localhost:3000/portal/dashboard`
- [ ] `http://localhost:3001/*`
- [ ] `http://localhost:3001/api/auth/callback`
- [ ] `http://localhost:3001/signin`
- [ ] `http://localhost:3001/dashboard`
- [ ] `http://localhost:8080/*`
- [ ] `http://localhost:8080/login/oauth2/code/keycloak`
- [ ] `http://localhost:8080/api/auth/callback`
- [ ] `https://aran-stg.zpoa.com/*`

### Post Logout Redirect URIs (5 total)
- [ ] `http://localhost:3000/*`
- [ ] `http://localhost:3000/signin`
- [ ] `http://localhost:3001/*`
- [ ] `http://localhost:3001/signin`
- [ ] `http://localhost:8080/*`

### Web Origins (4 total)
- [ ] `http://localhost:3000`
- [ ] `http://localhost:3001`
- [ ] `http://localhost:8080`
- [ ] `https://aran-stg.zpoa.com`

## 🔑 Credentials

- [ ] Navigate to **Credentials** tab
- [ ] Client Secret is: `LgDl38nUg6leUfB9DCG5LDglC75bxpOp`
- [ ] (Or new secret is updated in `application.yml`)

## 🗺️ Protocol Mappers

### Groups Mapper
- [ ] Name: `groups`
- [ ] Mapper Type: `Group Membership`
- [ ] Token Claim Name: `groups`
- [ ] Full group path: OFF
- [ ] Add to ID token: ON
- [ ] Add to access token: ON
- [ ] Add to userinfo: ON

### Email Mapper
- [ ] Name: `email`
- [ ] Mapper Type: `User Property`
- [ ] Property: `email`
- [ ] Token Claim Name: `email`
- [ ] Add to ID token: ON
- [ ] Add to access token: ON
- [ ] Add to userinfo: ON

### Given Name Mapper
- [ ] Name: `given name`
- [ ] Mapper Type: `User Property`
- [ ] Property: `firstName`
- [ ] Token Claim Name: `given_name`
- [ ] Add to ID token: ON
- [ ] Add to access token: ON
- [ ] Add to userinfo: ON

### Family Name Mapper
- [ ] Name: `family name`
- [ ] Mapper Type: `User Property`
- [ ] Property: `lastName`
- [ ] Token Claim Name: `family_name`
- [ ] Add to ID token: ON
- [ ] Add to access token: ON
- [ ] Add to userinfo: ON

### User Attributes Mapper (Optional)
- [ ] Name: `user-attributes`
- [ ] Mapper Type: `User Attribute`
- [ ] User Attribute: `*`
- [ ] Token Claim Name: `attributes`
- [ ] Multivalued: ON
- [ ] Aggregate attribute values: ON

## 👤 Service Account Roles

- [ ] Navigate to **Service Account Roles** tab
- [ ] Client Roles dropdown → Select `realm-management`
- [ ] Role `manage-users` is assigned
- [ ] Role `view-users` is assigned
- [ ] Role `query-users` is assigned
- [ ] Role `query-groups` is assigned

## 📦 Client Scopes

- [ ] Navigate to **Client Scopes** tab
- [ ] `email` is in **Assigned Default Client Scopes**
- [ ] `profile` is in **Assigned Default Client Scopes**
- [ ] `roles` is in **Assigned Default Client Scopes**
- [ ] `web-origins` is in **Assigned Default Client Scopes**

## 🧪 API Tests

### Test 1: Client Credentials Grant
```bash
curl -X POST "https://aran-stg.zpoa.com/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=ciyex-app" \
  -d "client_secret=LgDl38nUg6leUfB9DCG5LDglC75bxpOp"
```

- [ ] Returns HTTP 200
- [ ] Response contains `access_token`
- [ ] Response contains `expires_in`
- [ ] Response contains `token_type: Bearer`

### Test 2: Password Grant (User Login)
```bash
curl -X POST "https://aran-stg.zpoa.com/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=ciyex-app" \
  -d "client_secret=LgDl38nUg6leUfB9DCG5LDglC75bxpOp" \
  -d "username=aran-admin" \
  -d "password=Kc@2024!Secure#Pass"
```

- [ ] Returns HTTP 200
- [ ] Response contains `access_token`
- [ ] Response contains `refresh_token`
- [ ] Response contains `id_token`

### Test 3: Token Verification
Copy `access_token` from Test 2 and paste at https://jwt.io

- [ ] Token is valid (not expired)
- [ ] Contains `email` claim
- [ ] Contains `given_name` claim
- [ ] Contains `family_name` claim
- [ ] Contains `groups` claim (array)
- [ ] Contains `preferred_username` claim

## 🖥️ Application Tests

### Backend Configuration
- [ ] `application.yml` has correct Keycloak URL
- [ ] `application.yml` has correct realm name
- [ ] `application.yml` has correct client ID
- [ ] `application.yml` has correct client secret
- [ ] Backend starts without Keycloak errors

### Frontend Configuration (EHR UI)
- [ ] `.env.local` exists in `ciyex-ehr-ui/`
- [ ] `NEXT_PUBLIC_KEYCLOAK_ENABLED=true`
- [ ] `NEXT_PUBLIC_KEYCLOAK_URL=https://aran-stg.zpoa.com`
- [ ] `NEXT_PUBLIC_KEYCLOAK_REALM=master`
- [ ] `NEXT_PUBLIC_KEYCLOAK_CLIENT_ID=ciyex-app`

### Frontend Configuration (Portal UI)
- [ ] `.env.local` exists in `ciyex-portal-ui/`
- [ ] Keycloak settings match EHR UI

### Integration Test
1. Start all services:
   ```bash
   ./start-all.sh
   ```

- [ ] Backend starts on port 8080
- [ ] EHR UI starts on port 3000
- [ ] Portal UI starts on port 3001
- [ ] No startup errors in logs

2. Test EHR UI Login:
- [ ] Navigate to http://localhost:3000
- [ ] Can see "Aran ID (Keycloak)" button
- [ ] Click redirects to Keycloak login
- [ ] Can enter credentials
- [ ] Successfully redirects back to application
- [ ] User is logged in
- [ ] Groups are stored in localStorage

3. Test Portal UI Login:
- [ ] Navigate to http://localhost:3001
- [ ] Keycloak login works
- [ ] Successfully redirects back
- [ ] User is logged in

4. Test API Access:
- [ ] Can access protected endpoints with token
- [ ] Backend recognizes Keycloak token
- [ ] Groups are extracted from token

## 🔄 Token Lifecycle

- [ ] Can refresh token using refresh_token
- [ ] Token expires after configured time
- [ ] Refresh token works before expiry
- [ ] Can logout and token is invalidated

## 🚨 Error Handling

- [ ] Invalid credentials show proper error
- [ ] Expired token shows proper error
- [ ] Invalid redirect_uri shows proper error
- [ ] CORS errors are not occurring
- [ ] No "Service Accounts not enabled" errors
- [ ] No "Insufficient permissions" errors

## 📊 Monitoring

- [ ] Can view login events in Keycloak
- [ ] Can view admin events in Keycloak
- [ ] Backend logs show Keycloak authentication
- [ ] Frontend logs show successful token acquisition

## 🔒 Security Checklist

- [ ] Client secret is not hardcoded (uses env var)
- [ ] HTTPS is used in production
- [ ] Token expiration times are reasonable
- [ ] Refresh token rotation is enabled (optional)
- [ ] Service account has minimal required permissions
- [ ] Web origins are restricted to known domains
- [ ] Redirect URIs don't use wildcards excessively

## 📝 Documentation

- [ ] Setup guide is complete
- [ ] Team knows how to access Keycloak
- [ ] Team knows how to add users
- [ ] Team knows how to create groups
- [ ] Team knows how to assign roles

## ✅ Final Verification

- [ ] All checklist items above are complete
- [ ] Both EHR and Portal UIs work with Keycloak
- [ ] Backend properly validates Keycloak tokens
- [ ] Users can login and logout successfully
- [ ] Groups are properly included in tokens
- [ ] Service account can manage users
- [ ] No errors in application or Keycloak logs

---

## 🎯 Sign-off

**Verified by**: ________________  
**Date**: ________________  
**Environment**: □ Local  □ Development  □ Staging  □ Production

**Notes**:
_______________________________________________________
_______________________________________________________
_______________________________________________________

---

**Related Documentation**:
- Setup Guide: `KEYCLOAK_CLIENT_SETUP_GUIDE.md`
- Quick Reference: `KEYCLOAK_CLIENT_QUICK_REF.md`
- Integration Guide: `KEYCLOAK_INTEGRATION.md`
- Quick Start: `KEYCLOAK_QUICK_START.md`
