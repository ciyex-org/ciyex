# Keycloak Client Configuration - Quick Reference

## 🔑 Client Details

```
Keycloak Server: https://aran-stg.zpoa.com
Realm: master
Client ID: ciyex-app
Client Secret: LgDl38nUg6leUfB9DCG5LDglC75bxpOp
```

## 🌐 Application URLs

```
Backend API:  http://localhost:8080
EHR UI:       http://localhost:3000
Portal UI:    http://localhost:3001
```

## 🔧 Quick Setup Options

### Option 1: Manual Setup (Recommended for first time)
Follow the comprehensive guide: `KEYCLOAK_CLIENT_SETUP_GUIDE.md`

### Option 2: Automated Setup (Fast)
```bash
cd /Users/siva/git/ciyex
./scripts/setup-keycloak-client.sh
```

### Option 3: Using Keycloak Admin CLI
```bash
# Import client configuration
kcadm.sh create clients -r master -f scripts/keycloak-client-config.json
```

## 📋 All Redirect URIs (14 total)

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

## 🚪 Post Logout Redirect URIs (5 total)

```
http://localhost:3000/*
http://localhost:3000/signin
http://localhost:3001/*
http://localhost:3001/signin
http://localhost:8080/*
```

## 🌍 Web Origins (4 total)

```
http://localhost:3000
http://localhost:3001
http://localhost:8080
https://aran-stg.zpoa.com
```

## 🧪 Quick Tests

### Test Client Credentials
```bash
curl -X POST "https://aran-stg.zpoa.com/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=ciyex-app" \
  -d "client_secret=LgDl38nUg6leUfB9DCG5LDglC75bxpOp"
```

### Test User Login
```bash
curl -X POST "https://aran-stg.zpoa.com/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=ciyex-app" \
  -d "client_secret=LgDl38nUg6leUfB9DCG5LDglC75bxpOp" \
  -d "username=aran-admin" \
  -d "password=Kc@2024!Secure#Pass"
```

## 🔐 Required Client Settings

```yaml
Access Type: confidential
Standard Flow: ON
Direct Access Grants: ON
Service Accounts: ON
Authorization: ON
Implicit Flow: OFF
```

## 🎯 Required Service Account Roles

From `realm-management` client:
- ✅ manage-users
- ✅ view-users
- ✅ query-users
- ✅ query-groups

## 🗺️ Required Protocol Mappers

1. **groups** - Group Membership Mapper
2. **email** - User Property Mapper
3. **given_name** - User Property Mapper (firstName)
4. **family_name** - User Property Mapper (lastName)
5. **user-attributes** - User Attribute Mapper

## ⚡ Quick Start Commands

```bash
# Start all services
./start-all.sh

# Or start individually
./start-backend.sh    # Port 8080
./start-ehr-ui.sh     # Port 3000
./start-portal-ui.sh  # Port 3001
```

## 🔍 Verify Token Claims

Paste your access token at: https://jwt.io

Expected claims:
```json
{
  "email": "user@example.com",
  "given_name": "John",
  "family_name": "Doe",
  "groups": ["doctors", "admin"],
  "preferred_username": "john.doe"
}
```

## 🆘 Common Issues

### Invalid redirect_uri
→ Add the URI to Valid Redirect URIs in Keycloak

### Invalid client credentials
→ Verify client secret matches application.yml

### Groups not in token
→ Configure groups mapper in Keycloak

### CORS errors
→ Add origins to Web Origins in Keycloak

## 📚 Full Documentation

- Complete Setup: `KEYCLOAK_CLIENT_SETUP_GUIDE.md`
- Quick Start: `KEYCLOAK_QUICK_START.md`
- Integration: `KEYCLOAK_INTEGRATION.md`
- Client Credentials: `KEYCLOAK_CLIENT_CREDENTIALS.md`

---

**Last Updated**: October 28, 2025
