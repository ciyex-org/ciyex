# Keycloak Client Setup Guide - Complete Configuration

## 📋 Overview

This guide provides step-by-step instructions to create and configure the `ciyex-app` Keycloak client with all required redirect URLs based on your `application.yml` configuration.

## 🔑 Client Configuration Details

Based on your `application.yml`:

```yaml
Keycloak Server: https://aran-stg.zpoa.com
Realm: master
Client ID: ciyex-app
Client Secret: LgDl38nUg6leUfB9DCG5LDglC75bxpOp
Admin Username: aran-admin
Admin Password: Kc@2024!Secure#Pass
```

Application URLs:
- **Backend API**: http://localhost:8080
- **EHR UI**: http://localhost:3000
- **Portal UI**: http://localhost:3001

---

## 🚀 Step-by-Step Client Setup

### 1. Access Keycloak Admin Console

1. Navigate to: **https://aran-stg.zpoa.com**
2. Click on **Administration Console**
3. Login with:
   - **Username**: `aran-admin`
   - **Password**: `Kc@2024!Secure#Pass`
4. Select **Realm**: `master` (top-left dropdown)

---

### 2. Create the Client (if not exists)

1. In the left sidebar, click **Clients**
2. Click **Create** button (top right)
3. Fill in the form:

```
Client ID: ciyex-app
Client Protocol: openid-connect
Root URL: (leave empty)
```

4. Click **Save**

---

### 3. Configure Client Settings

Navigate to the **Settings** tab of `ciyex-app` client:

#### General Settings

```
Client ID: ciyex-app
Name: Ciyex Application
Description: Ciyex EHR and Portal Application
Enabled: ON
```

#### Access Settings

```
Client Protocol: openid-connect
Access Type: confidential
Standard Flow Enabled: ON
Implicit Flow Enabled: OFF
Direct Access Grants Enabled: ON
Service Accounts Enabled: ON
Authorization Enabled: ON
```

#### Login Settings - Valid Redirect URIs

Add **ALL** of the following redirect URIs (click **+** to add each one):

```
http://localhost:3000/*
http://localhost:3000/api/auth/callback
http://localhost:3000/signin
http://localhost:3000/dashboard
http://localhost:3000/select-practice
http://localhost:3000/portal/dashboard
http://localhost:3001/*
http://localhost:3001/api/auth/callback
http://localhost:3001/signin
http://localhost:3001/dashboard
http://localhost:8080/*
http://localhost:8080/login/oauth2/code/keycloak
http://localhost:8080/api/auth/callback
https://aran-stg.zpoa.com/*
```

#### Login Settings - Valid Post Logout Redirect URIs

```
http://localhost:3000/*
http://localhost:3000/signin
http://localhost:3001/*
http://localhost:3001/signin
http://localhost:8080/*
```

#### Login Settings - Web Origins

```
http://localhost:3000
http://localhost:3001
http://localhost:8080
https://aran-stg.zpoa.com
```

#### Advanced Settings

```
Access Token Lifespan: 5 Minutes (300 seconds)
Client Session Idle: 30 Minutes (1800 seconds)
Client Session Max: 10 Hours (36000 seconds)
```

5. Click **Save** at the bottom

---

### 4. Get/Set Client Secret

1. Navigate to the **Credentials** tab
2. You should see the **Secret** field
3. The current secret should be: `LgDl38nUg6leUfB9DCG5LDglC75bxpOp`

If you need to regenerate:
- Click **Regenerate Secret**
- Copy the new secret
- Update it in your `application.yml`:

```yaml
keycloak:
  credentials:
    secret: ${KEYCLOAK_CLIENT_SECRET:YOUR_NEW_SECRET_HERE}
```

---

### 5. Configure Service Account Roles

Since `Service Accounts Enabled: ON`, the client needs roles to manage users.

1. Navigate to the **Service Account Roles** tab
2. In the **Client Roles** dropdown, select **realm-management**
3. Select and add the following roles from **Available Roles** to **Assigned Roles**:
   - ✅ `manage-users`
   - ✅ `view-users`
   - ✅ `manage-clients`
   - ✅ `view-clients`
   - ✅ `query-users`
   - ✅ `query-groups`

4. Click **Add selected**

---

### 6. Configure Mappers for User Info

To include user groups and attributes in the JWT token:

1. Navigate to the **Mappers** tab
2. Click **Create** to add mappers

#### Mapper 1: Groups Mapper

```
Name: groups
Mapper Type: Group Membership
Token Claim Name: groups
Full group path: OFF
Add to ID token: ON
Add to access token: ON
Add to userinfo: ON
```

Click **Save**

#### Mapper 2: Email Mapper (verify exists)

```
Name: email
Mapper Type: User Property
Property: email
Token Claim Name: email
Claim JSON Type: String
Add to ID token: ON
Add to access token: ON
Add to userinfo: ON
```

Click **Save**

#### Mapper 3: Given Name

```
Name: given name
Mapper Type: User Property
Property: firstName
Token Claim Name: given_name
Claim JSON Type: String
Add to ID token: ON
Add to access token: ON
Add to userinfo: ON
```

Click **Save**

#### Mapper 4: Family Name

```
Name: family name
Mapper Type: User Property
Property: lastName
Token Claim Name: family_name
Claim JSON Type: String
Add to ID token: ON
Add to access token: ON
Add to userinfo: ON
```

Click **Save**

#### Mapper 5: User Attributes (for orgId, facilityId, etc.)

```
Name: user-attributes
Mapper Type: User Attribute
User Attribute: *
Token Claim Name: attributes
Claim JSON Type: JSON
Add to ID token: ON
Add to access token: ON
Add to userinfo: ON
Multivalued: ON
Aggregate attribute values: ON
```

Click **Save**

---

### 7. Configure Scope Settings

1. Navigate to the **Client Scopes** tab
2. Ensure the following scopes are in **Assigned Default Client Scopes**:
   - ✅ `email`
   - ✅ `profile`
   - ✅ `roles`
   - ✅ `web-origins`

If any are missing:
- Find them in **Available Client Scopes**
- Click **Add selected** to move to **Assigned Default Client Scopes**

---

### 8. Configure Roles (Optional)

If you want to define application-specific roles:

1. Navigate to the **Roles** tab of the client
2. Click **Add Role**
3. Create roles such as:
   - `patient`
   - `doctor`
   - `admin`
   - `nurse`
   - `staff`

---

## 🧪 Testing the Configuration

### Test 1: Get Access Token with Client Credentials

```bash
curl -X POST "https://aran-stg.zpoa.com/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=ciyex-app" \
  -d "client_secret=LgDl38nUg6leUfB9DCG5LDglC75bxpOp"
```

**Expected Response:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 300,
  "token_type": "Bearer",
  "scope": "profile email"
}
```

### Test 2: User Login with Password Grant

```bash
curl -X POST "https://aran-stg.zpoa.com/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=ciyex-app" \
  -d "client_secret=LgDl38nUg6leUfB9DCG5LDglC75bxpOp" \
  -d "username=aran-admin" \
  -d "password=Kc@2024!Secure#Pass"
```

**Expected Response:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 300,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "scope": "profile email"
}
```

### Test 3: Frontend Authentication Flow

1. Start your applications:
   ```bash
   # Terminal 1 - Backend
   ./start-backend.sh
   
   # Terminal 2 - EHR UI
   ./start-ehr-ui.sh
   
   # Terminal 3 - Portal UI
   ./start-portal-ui.sh
   ```

2. Navigate to http://localhost:3000
3. Click "Aran ID (Keycloak)" button
4. You should be redirected to Keycloak login
5. Enter credentials and verify successful redirect back

### Test 4: Verify Token Claims

1. Copy the `access_token` from Test 2
2. Go to https://jwt.io
3. Paste the token
4. Verify the payload contains:
   ```json
   {
     "email": "...",
     "given_name": "...",
     "family_name": "...",
     "groups": ["...", "..."],
     "preferred_username": "..."
   }
   ```

---

## 📝 Complete Redirect URLs Summary

Here's the complete list of all redirect URLs configured:

### Valid Redirect URIs (16 entries)
```
1.  http://localhost:3000/*
2.  http://localhost:3000/api/auth/callback
3.  http://localhost:3000/signin
4.  http://localhost:3000/dashboard
5.  http://localhost:3000/select-practice
6.  http://localhost:3000/portal/dashboard
7.  http://localhost:3001/*
8.  http://localhost:3001/api/auth/callback
9.  http://localhost:3001/signin
10. http://localhost:3001/dashboard
11. http://localhost:8080/*
12. http://localhost:8080/login/oauth2/code/keycloak
13. http://localhost:8080/api/auth/callback
14. https://aran-stg.zpoa.com/*
```

### Valid Post Logout Redirect URIs (6 entries)
```
1. http://localhost:3000/*
2. http://localhost:3000/signin
3. http://localhost:3001/*
4. http://localhost:3001/signin
5. http://localhost:8080/*
```

### Web Origins (4 entries)
```
1. http://localhost:3000
2. http://localhost:3001
3. http://localhost:8080
4. https://aran-stg.zpoa.com
```

---

## 🔐 Production Configuration

When deploying to production, you'll need to add production URLs:

### Production URLs to Add

```
Valid Redirect URIs:
- https://your-domain.com/*
- https://your-domain.com/api/auth/callback
- https://your-domain.com/signin
- https://your-domain.com/dashboard
- https://your-domain.com/select-practice
- https://portal.your-domain.com/*
- https://portal.your-domain.com/api/auth/callback
- https://portal.your-domain.com/signin

Valid Post Logout Redirect URIs:
- https://your-domain.com/*
- https://your-domain.com/signin
- https://portal.your-domain.com/*
- https://portal.your-domain.com/signin

Web Origins:
- https://your-domain.com
- https://portal.your-domain.com
```

---

## 🛠️ Troubleshooting

### Error: "Invalid redirect_uri"

**Cause**: The redirect URI is not in the allowed list

**Solution**: 
1. Check the exact URI being used (check browser console)
2. Add it to **Valid Redirect URIs** in Keycloak
3. Ensure wildcards are used correctly (`*` at the end)

### Error: "Invalid client credentials"

**Cause**: Client secret mismatch

**Solution**:
1. Go to Keycloak → Clients → ciyex-app → Credentials
2. Copy the secret
3. Update `application.yml`:
   ```yaml
   keycloak:
     credentials:
       secret: <copied-secret>
   ```

### Error: "Insufficient permissions"

**Cause**: Service account missing required roles

**Solution**:
1. Go to Keycloak → Clients → ciyex-app → Service Account Roles
2. Select **realm-management** in Client Roles dropdown
3. Add: `manage-users`, `view-users`, `query-users`

### Error: "Groups not in token"

**Cause**: Groups mapper not configured

**Solution**:
1. Go to Keycloak → Clients → ciyex-app → Mappers
2. Create/verify "groups" mapper (see step 6 above)
3. Ensure "Add to access token" is ON

### CORS Errors

**Cause**: Web origins not configured

**Solution**:
1. Go to Keycloak → Clients → ciyex-app → Settings
2. Add all origins to **Web Origins**
3. Save

---

## ✅ Verification Checklist

After completing the setup, verify:

- [ ] Client `ciyex-app` exists in `master` realm
- [ ] Access Type is set to **confidential**
- [ ] Service Accounts Enabled is **ON**
- [ ] All 14 redirect URIs are configured
- [ ] All 5 post logout URIs are configured
- [ ] All 4 web origins are configured
- [ ] Client secret matches `application.yml`
- [ ] Service account has `manage-users` and `view-users` roles
- [ ] Groups mapper is configured
- [ ] Email, given_name, family_name mappers exist
- [ ] Can get access token with client credentials
- [ ] Can login with username/password
- [ ] Frontend redirect works correctly
- [ ] Token contains groups and user info

---

## 📚 Related Documentation

- **Quick Start**: `KEYCLOAK_QUICK_START.md`
- **Integration Guide**: `KEYCLOAK_INTEGRATION.md`
- **Client Credentials**: `KEYCLOAK_CLIENT_CREDENTIALS.md`
- **SSO Setup**: `KEYCLOAK_SSO_SETUP.md`

---

## 🎯 Next Steps

1. ✅ Complete client setup in Keycloak
2. ✅ Test authentication flows
3. Create test users in Keycloak
4. Create and configure groups
5. Assign users to groups
6. Test group-based access control
7. Configure production URLs when ready

---

**Status**: Configuration guide complete ✅  
**Last Updated**: October 28, 2025  
**Keycloak Version**: Compatible with Keycloak 20+

