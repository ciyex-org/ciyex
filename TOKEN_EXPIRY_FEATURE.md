# Token Expiry Customization Feature

This feature allows organizations to customize their session token expiration time from 5 to 30 minutes, with real-time integration to Keycloak.

## 🚀 Features

- **Customizable Token Expiry**: Set token expiration between 5-30 minutes per organization
- **Real-time Keycloak Integration**: Updates are applied immediately to Keycloak realm settings
- **Practice-based Configuration**: Each practice can have its own token expiry settings
- **Comprehensive API**: RESTful endpoints for getting and setting token expiry
- **Frontend Component**: Ready-to-use JavaScript component for UI integration
- **Testing Endpoints**: Built-in endpoints to test and verify functionality

## 📋 API Endpoints

### Settings Endpoints

#### Get Current Token Expiry
```http
GET /api/settings/token-expiry
```

**Response:**
```json
{
  "success": true,
  "message": "Token expiry retrieved successfully",
  "data": {
    "minutes": 15,
    "keycloakUpdated": true,
    "keycloakMessage": "Current setting"
  }
}
```

#### Update Token Expiry
```http
POST /api/settings/token-expiry
Content-Type: application/json

{
  "minutes": 20
}
```

**Response:**
```json
{
  "success": true,
  "message": "Token expiry updated successfully",
  "data": {
    "minutes": 20,
    "keycloakUpdated": true,
    "keycloakMessage": "Keycloak updated successfully"
  }
}
```

#### Get Practice Settings (includes token expiry)
```http
GET /api/settings/practice/settings
```

#### Update Practice Settings (includes token expiry)
```http
POST /api/settings/practice/settings
Content-Type: application/json

{
  "sessionTimeoutMinutes": 25
}
```

### Test Endpoints

#### System Status Check
```http
GET /api/test/token-expiry/status
```

#### Simulate Token Expiry Update
```http
POST /api/test/token-expiry/simulate
Content-Type: application/json

{
  "minutes": 15
}
```

#### Test Keycloak Connection
```http
GET /api/test/token-expiry/keycloak-connection
```

## 🔧 Configuration

### Required Keycloak Configuration

Add these properties to your `application.yml`:

```yaml
keycloak:
  auth-server-url: https://your-keycloak-server.com
  realm: your-realm-name
  resource: your-client-id
  credentials:
    secret: your-client-secret
  admin:
    username: admin-username
    password: admin-password
```

### Database Schema

The `practice` table already includes the required column:

```sql
ALTER TABLE practice ADD COLUMN token_expiry_minutes INTEGER DEFAULT 5;
```

## 🎨 Frontend Integration

### Using the JavaScript Component

1. Include the component script:
```html
<script src="/js/token-expiry-settings.js"></script>
```

2. Create a container element:
```html
<div id="tokenExpiryContainer"></div>
```

3. Initialize the component:
```javascript
const tokenSettings = new TokenExpirySettings('tokenExpiryContainer', '/api');
```

### Example Integration

See `public/practice-settings.html` for a complete example.

## 🔒 Security Considerations

- **Validation**: Token expiry is validated to be between 5-30 minutes
- **Admin Access**: Keycloak admin credentials are required for realm updates
- **Audit Logging**: All changes are logged for audit purposes
- **Graceful Degradation**: If Keycloak update fails, practice settings are still saved

## 🧪 Testing

### Manual Testing

1. **Test System Status:**
```bash
curl -X GET http://localhost:8080/api/test/token-expiry/status
```

2. **Test Keycloak Connection:**
```bash
curl -X GET http://localhost:8080/api/test/token-expiry/keycloak-connection
```

3. **Simulate Update:**
```bash
curl -X POST http://localhost:8080/api/test/token-expiry/simulate \
  -H "Content-Type: application/json" \
  -d '{"minutes": 15}'
```

4. **Update Token Expiry:**
```bash
curl -X POST http://localhost:8080/api/settings/token-expiry \
  -H "Content-Type: application/json" \
  -d '{"minutes": 20}'
```

### Expected Behavior

- **Valid Range**: Only accepts values between 5-30 minutes
- **Keycloak Updates**: Updates multiple Keycloak settings:
  - `accessTokenLifespan`
  - `ssoSessionIdleTimeout`
  - `ssoSessionMaxLifespan`
  - `accessTokenLifespanForImplicitFlow`
  - `offlineSessionIdleTimeout`
- **Error Handling**: Gracefully handles Keycloak connection failures
- **Immediate Effect**: Changes take effect immediately for new logins

## 🔍 Troubleshooting

### Common Issues

1. **Keycloak Connection Failed**
   - Check Keycloak server URL and credentials
   - Verify admin user has realm management permissions
   - Check network connectivity

2. **Practice Not Found**
   - Ensure at least one practice exists in the database
   - Check practice service configuration

3. **Invalid Token Expiry Range**
   - Verify the value is between 5-30 minutes
   - Check request payload format

### Logs

Monitor these log messages:
- `✅ Successfully updated Keycloak realm token lifespan`
- `⚠️ Failed to update Keycloak token lifespan`
- `❌ Error updating Keycloak`

## 🚀 Deployment

1. **Build the application:**
```bash
./gradlew build
```

2. **Update Keycloak configuration** in your environment
3. **Deploy the application**
4. **Test the endpoints** using the test controller
5. **Integrate the frontend component** into your existing UI

## 📝 Notes

- Changes affect all users in the organization
- Existing sessions continue until their current expiry time
- New logins will use the updated token expiry time
- The system maintains backward compatibility with existing session timeout settings
- Frontend component is framework-agnostic and can be integrated into any web application

## 🔄 Migration

If upgrading from an existing system:

1. Existing `sessionTimeoutMinutes` values will be mapped to `tokenExpiryMinutes`
2. Default value is 5 minutes if not specified
3. No data migration is required - the system handles defaults automatically