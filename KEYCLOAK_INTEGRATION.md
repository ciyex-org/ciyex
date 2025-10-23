# Keycloak (Aran ID) Integration Guide

## Overview

This document describes the integration of Keycloak authentication (Aran ID) into the Ciyex EHR system. The integration replaces the local tenant-based authentication with Keycloak groups-based authentication.

## Keycloak Configuration

### Server Details
- **URL**: https://aran-stg.zpoa.com/
- **Realm**: master
- **Admin Username**: aran-admin
- **Admin Password**: Kc@2024!Secure#Pass
- **Client ID**: ciyex-app

### Key Changes
- **Tenant → Groups**: The traditional tenant/organization concept has been replaced with Keycloak groups
- **Authentication Methods**: System now supports both Keycloak and local authentication
- **Token Management**: JWT tokens from Keycloak are used for authentication

## Backend Configuration

### 1. Dependencies Added (build.gradle)
```gradle
implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
```

### 2. Application Configuration (application.yml)
```yaml
# Keycloak Configuration
keycloak:
  enabled: true
  auth-server-url: https://aran-stg.zpoa.com/
  realm: master
  resource: ciyex-app
  credentials:
    secret: ${KEYCLOAK_CLIENT_SECRET:LgDl38nUg6leUfB9DCG5LDglC75bxpOp}
  use-resource-role-mappings: true
  bearer-only: false

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://aran-stg.zpoa.com/realms/master
          jwk-set-uri: https://aran-stg.zpoa.com/realms/master/protocol/openid-connect/certs
```

### 3. New Backend Components

#### KeycloakConfig.java
Configuration class for Keycloak settings with helper methods for endpoints.

#### KeycloakAuthService.java
Service handling:
- User authentication with Keycloak
- Token retrieval and validation
- User info extraction
- Group extraction (replacing tenant logic)
- Logout functionality

#### KeycloakJwtAuthenticationConverter.java
Converts Keycloak JWT tokens to Spring Security Authentication:
- Maps Keycloak groups to Spring Security authorities with `GROUP_` prefix
- Maps realm roles to authorities with `ROLE_` prefix
- Extracts user information from JWT claims

#### SecurityConfig.java (Updated)
- Added OAuth2 Resource Server support
- Configured JWT authentication converter
- Added `/api/auth/keycloak-login` endpoint to public access

#### AuthController.java (Updated)
New endpoint: `POST /api/auth/keycloak-login`
- Accepts username and password
- Authenticates with Keycloak
- Returns access token, refresh token, and user info
- Returns groups instead of tenant/org data

## Frontend Configuration

### 1. Environment Variables (.env.local)
```bash
NEXT_PUBLIC_API_URL=http://localhost:8080
PORT=3000

# Keycloak Configuration
NEXT_PUBLIC_KEYCLOAK_ENABLED=true
NEXT_PUBLIC_KEYCLOAK_URL=https://aran-stg.zpoa.com
NEXT_PUBLIC_KEYCLOAK_REALM=master
NEXT_PUBLIC_KEYCLOAK_CLIENT_ID=ciyex-app
```

### 2. SignInForm Component (Updated)
- Added toggle between Local and Keycloak authentication
- Updated to handle both authentication methods
- Stores groups instead of orgIds for Keycloak auth
- Maintains backward compatibility with local auth

#### Local Storage Keys (Keycloak Auth)
- `token`: Access token from Keycloak
- `userEmail`: User's email or username
- `userFullName`: User's full name
- `userId`: Keycloak user ID (sub claim)
- `groups`: JSON array of user's groups
- `primaryGroup`: First group (for backward compatibility)
- `authMethod`: "keycloak" or "local"

## API Endpoints

### Keycloak Login
```http
POST /api/auth/keycloak-login
Content-Type: application/json

{
  "username": "user@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Keycloak login successful",
  "data": {
    "token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expires_in": 300,
    "email": "user@example.com",
    "username": "user",
    "firstName": "John",
    "lastName": "Doe",
    "groups": ["doctors", "admin"],
    "userId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
  }
}
```

### Local Login (Existing)
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

## Groups vs Tenant Mapping

### Traditional Tenant Model
```
User → Organization → Facilities → Roles
```

### New Keycloak Groups Model
```
User → Groups (e.g., "doctors", "nurses", "admin", "org-hospital-a")
```

### Group Naming Convention
Recommended group naming for organizations:
- `org-{organization-name}` - Organization membership
- `role-{role-name}` - Role-based groups
- `facility-{facility-name}` - Facility access

Example groups:
- `org-qiaben-health`
- `role-doctor`
- `role-admin`
- `facility-main-clinic`

## Security Features

### JWT Token Validation
- Tokens are validated against Keycloak's public keys
- Token expiration is enforced
- Issuer validation ensures tokens are from correct Keycloak realm

### Authority Mapping
- Keycloak groups → `GROUP_{GROUP_NAME}` authorities
- Realm roles → `ROLE_{ROLE_NAME}` authorities
- Client roles → `ROLE_{ROLE_NAME}` authorities

### Dual Authentication Support
The system supports both authentication methods simultaneously:
- **Keycloak**: For SSO and centralized user management
- **Local**: For backward compatibility and offline scenarios

## Setup Instructions

### 1. Keycloak Server Setup
1. Access Keycloak admin console: https://aran-stg.zpoa.com/
2. Login with admin credentials
3. Create client `ciyex-app` if not exists
4. Configure client settings:
   - Access Type: confidential
   - Standard Flow Enabled: ON
   - Direct Access Grants Enabled: ON
   - Valid Redirect URIs: http://localhost:3000/*
5. Note the client secret from Credentials tab

### 2. Backend Setup
1. Set environment variable:
   ```bash
   export KEYCLOAK_CLIENT_SECRET=your-client-secret-here
   ```
2. Build the application:
   ```bash
   ./gradlew clean build
   ```
3. Run the application:
   ```bash
   ./gradlew bootRun
   ```

### 3. Frontend Setup
1. Update `.env.local` with Keycloak settings
2. Install dependencies:
   ```bash
   cd ciyex-ehr-ui
   npm install
   ```
3. Run development server:
   ```bash
   npm run dev
   ```

## Testing

### Test Keycloak Authentication
1. Navigate to login page
2. Click "Aran ID (Keycloak)" button
3. Enter Keycloak credentials
4. Verify successful login and group assignment

### Test Local Authentication
1. Navigate to login page
2. Click "Local Login" button
3. Enter local credentials
4. Verify successful login with org/facility assignment

## Troubleshooting

### Common Issues

#### 1. "Keycloak authentication is not enabled"
- Ensure `keycloak.enabled=true` in application.yml
- Check environment variables are loaded

#### 2. "Invalid Keycloak credentials"
- Verify username/password are correct in Keycloak
- Check user is enabled in Keycloak admin console

#### 3. JWT validation errors
- Verify `issuer-uri` matches Keycloak realm URL
- Check system time is synchronized (JWT exp validation)
- Ensure Keycloak server is accessible

#### 4. Groups not appearing
- Verify user is assigned to groups in Keycloak
- Check group mapper is configured in client
- Add groups to token claims in client mappers

### Debug Logging
Enable debug logging in application.yml:
```yaml
logging:
  level:
    com.qiaben.ciyex.service.KeycloakAuthService: DEBUG
    com.qiaben.ciyex.security: DEBUG
    org.springframework.security: DEBUG
```

## Migration Guide

### Migrating from Tenant to Groups

1. **Map existing organizations to Keycloak groups**
   - Create groups in Keycloak for each organization
   - Use naming convention: `org-{organization-name}`

2. **Migrate users**
   - Create users in Keycloak
   - Assign appropriate groups
   - Maintain email as unique identifier

3. **Update application logic**
   - Replace tenant checks with group checks
   - Use `localStorage.getItem("groups")` instead of `orgId`
   - Check for group membership: `groups.includes("org-name")`

4. **Backward compatibility**
   - Keep local authentication enabled during transition
   - Gradually migrate users to Keycloak
   - Monitor both authentication methods

## Security Considerations

1. **Client Secret**: Store securely, never commit to version control
2. **Token Storage**: Tokens stored in localStorage (consider httpOnly cookies for production)
3. **HTTPS**: Always use HTTPS in production for Keycloak communication
4. **Token Expiration**: Implement token refresh logic for long sessions
5. **Group Validation**: Validate group membership on backend for sensitive operations

## Future Enhancements

- [ ] Implement token refresh mechanism
- [ ] Add social login providers through Keycloak
- [ ] Implement role-based access control using groups
- [ ] Add group management UI
- [ ] Implement SSO logout
- [ ] Add multi-factor authentication support
- [ ] Migrate to httpOnly cookies for token storage
