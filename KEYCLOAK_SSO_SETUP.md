# Keycloak SSO Integration - Sign In with Aran

## ✅ Changes Implemented

### 1. **Updated Sign-In Page**
Replaced the email/password form with a "Sign in with Aran" button that redirects to Keycloak.

**File:** `ciyex-ehr-ui/src/components/auth/SignInForm.tsx`

**Changes:**
- ❌ Removed email/password input fields
- ❌ Removed "Remember me" checkbox
- ❌ Removed "Forgot password" link
- ✅ Added "Sign in with Aran" button
- ✅ Redirects to Keycloak authentication page
- ✅ Shows loading state during redirect
- ✅ Displays error if Keycloak is not configured

### 2. **Added Keycloak Environment Variables**
**File:** `ciyex-ehr-ui/.env.local`

```env
# Keycloak Configuration
NEXT_PUBLIC_KEYCLOAK_ENABLED=true
NEXT_PUBLIC_KEYCLOAK_URL=https://aran-stg.zpoa.com
NEXT_PUBLIC_KEYCLOAK_REALM=master
NEXT_PUBLIC_KEYCLOAK_CLIENT_ID=ciyex-app
```

### 3. **Backend Keycloak Configuration**
**File:** `src/main/resources/application.yml`

```yaml
keycloak:
  enabled: ${KEYCLOAK_ENABLED:false}
  auth-server-url: ${KEYCLOAK_AUTH_SERVER_URL:https://aran-stg.zpoa.com}
  realm: ${KEYCLOAK_REALM:master}
  resource: ${KEYCLOAK_RESOURCE:ciyex-app}
  credentials:
    secret: ${KEYCLOAK_CLIENT_SECRET:LgDl38nUg6leUfB9DCG5LDglC75bxpOp}
```

## 🔄 Authentication Flow

### New Keycloak SSO Flow

1. **User visits sign-in page** → `/signin`
2. **Clicks "Sign in with Aran"** button
3. **Redirected to Keycloak** → `https://aran-stg.zpoa.com/realms/master/protocol/openid-connect/auth`
4. **User authenticates** on Aran/Keycloak
5. **Redirected back** → `/callback` with authorization code
6. **Frontend exchanges code** → Calls backend `/api/auth/keycloak-callback`
7. **Backend validates** → Exchanges code for JWT token
8. **User authenticated** → Redirected to `/dashboard`

## 📋 Sign-In Page Features

### UI Elements

**Title:** "Welcome to Ciyex EHR"

**Subtitle:** "Sign in with your Aran account to continue"

**Button:**
- Text: "Sign in with Aran"
- Icon: Info/Shield icon
- Loading state: "Redirecting to Aran..." with spinner
- Disabled when Keycloak not configured

**Error Message:**
- Shows if `NEXT_PUBLIC_KEYCLOAK_ENABLED` is not `true`
- Message: "Keycloak authentication is not configured. Please contact your administrator."

**Footer:**
- "By signing in, you agree to our Terms of Service and Privacy Policy"

## 🔧 Configuration

### Frontend Configuration

**Required Environment Variables:**
```env
NEXT_PUBLIC_KEYCLOAK_ENABLED=true
NEXT_PUBLIC_KEYCLOAK_URL=https://aran-stg.zpoa.com
NEXT_PUBLIC_KEYCLOAK_REALM=master
NEXT_PUBLIC_KEYCLOAK_CLIENT_ID=ciyex-app
```

### Backend Configuration

**Required Properties:**
```yaml
keycloak:
  enabled: true
  auth-server-url: https://aran-stg.zpoa.com
  realm: master
  resource: ciyex-app
  credentials:
    secret: <client-secret>
```

### Keycloak Client Configuration

**Required Settings in Keycloak:**
1. **Client ID:** `ciyex-app`
2. **Client Protocol:** `openid-connect`
3. **Access Type:** `confidential`
4. **Valid Redirect URIs:**
   - `http://localhost:3000/callback`
   - `https://your-domain.com/callback`
5. **Web Origins:** `*` or specific origins

## 🚀 Testing

### Test the Sign-In Flow

1. **Start the services:**
   ```bash
   # Backend
   SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
   
   # EHR UI
   cd ciyex-ehr-ui && npm run dev
   ```

2. **Navigate to sign-in:**
   ```
   http://localhost:3000/signin
   ```

3. **Click "Sign in with Aran"**
   - Should redirect to Keycloak login page
   - URL should be: `https://aran-stg.zpoa.com/realms/master/protocol/openid-connect/auth?...`

4. **Enter Keycloak credentials**
   - Use your Aran/Keycloak username and password

5. **Verify redirect back:**
   - Should redirect to: `http://localhost:3000/callback`
   - Then automatically to: `http://localhost:3000/dashboard`

6. **Check localStorage:**
   ```javascript
   localStorage.getItem('token')        // JWT token
   localStorage.getItem('authMethod')   // 'keycloak'
   localStorage.getItem('userEmail')    // User email
   localStorage.getItem('groups')       // User groups
   ```

## 🔍 Troubleshooting

### Issue: "Keycloak authentication is not configured"

**Solution:**
- Check `.env.local` file exists in `ciyex-ehr-ui/`
- Verify `NEXT_PUBLIC_KEYCLOAK_ENABLED=true`
- Restart the dev server: `npm run dev`

### Issue: Redirect fails or shows error

**Solution:**
- Verify redirect URI is configured in Keycloak client
- Check that callback endpoint exists: `/callback`
- Verify backend endpoint: `/api/auth/keycloak-callback`

### Issue: Backend callback fails

**Solution:**
- Check backend logs for errors
- Verify Keycloak client secret is correct
- Ensure backend can reach Keycloak URL

### Issue: Token not stored after callback

**Solution:**
- Check browser console for errors
- Verify callback page logic
- Check backend response format

## 📝 Code Changes Summary

### Files Modified

1. **`ciyex-ehr-ui/src/components/auth/SignInForm.tsx`**
   - Removed email/password form
   - Added Keycloak SSO button
   - Added redirect logic

2. **`ciyex-ehr-ui/.env.local`**
   - Added Keycloak environment variables

3. **`src/main/resources/application.yml`**
   - Added Keycloak configuration properties

### Files Already Present (No Changes Needed)

1. **`ciyex-ehr-ui/src/app/(full-width-pages)/(auth)/callback/page.tsx`**
   - Handles OAuth callback
   - Exchanges code for token
   - Already implemented ✅

2. **`ciyex-ehr-ui/src/utils/authUtils.ts`**
   - Utility functions for auth
   - Supports both Keycloak and local auth
   - Already implemented ✅

## 🎯 Benefits

✅ **Single Sign-On** - Users sign in once across all Ciyex applications
✅ **Centralized Auth** - All authentication managed in Keycloak/Aran
✅ **Better Security** - No passwords stored in application
✅ **User Management** - Add/remove users in Keycloak
✅ **Group-Based Access** - Control access via Keycloak groups
✅ **Seamless UX** - One-click sign-in experience

## 🔐 Security Notes

- JWT tokens are stored in localStorage
- Tokens expire based on Keycloak configuration
- Backend validates all tokens with Keycloak
- HTTPS required for production
- Client secret must be kept secure

## 📚 Related Documentation

- **Keycloak Integration:** `KEYCLOAK_INTEGRATION.md`
- **Keycloak Quick Start:** `KEYCLOAK_QUICK_START.md`
- **Keycloak Changes:** `KEYCLOAK_CHANGES_SUMMARY.md`
- **Auth Utilities:** `ciyex-ehr-ui/src/utils/authUtils.ts`

---

**Last Updated:** October 23, 2025
**Status:** ✅ Implemented and Ready for Testing
