# Jitsi Telehealth Configuration Guide

## Prerequisites

1. **Jitsi Meet Server**: Running at https://meet-stg.ciyex.com/
2. **Java Backend**: Spring Boot application with telehealth service
3. **EHR UI**: Next.js application with React components

## Step 1: Backend Configuration

### 1.1 Update application.yml
Add the following configuration to your `src/main/resources/application.yml`:

```yaml
telehealth:
  providers:
    jitsi:
      serverUrl: "https://meet-stg.ciyex.com"
      appId: "your-jitsi-app-id"        # Replace with your Jitsi app ID
      appSecret: "your-jwt-secret-key"   # Replace with your JWT secret
```

### 1.2 Environment Variables (Alternative)
You can also use environment variables:

```bash
TELEHEALTH_JITSI_SERVER_URL=https://meet-stg.ciyex.com
TELEHEALTH_JITSI_APP_ID=your-jitsi-app-id
TELEHEALTH_JITSI_APP_SECRET=your-jwt-secret-key
```

## Step 2: Jitsi Server Configuration

### 2.1 JWT Authentication Setup
Your Jitsi server needs to be configured for JWT authentication. Add to your Jitsi configuration:

**prosody.cfg.lua**:
```lua
VirtualHost "meet-stg.ciyex.com"
    authentication = "token"
    app_id = "your-jitsi-app-id"
    app_secret = "your-jwt-secret-key"
    allow_empty_token = false
```

**jicofo.conf**:
```
jicofo {
  authentication: {
    enabled: true
    type: JWT
  }
}
```

### 2.2 CORS Configuration
Ensure your Jitsi server allows requests from your EHR UI domain:

**nginx/apache configuration**:
```
Access-Control-Allow-Origin: https://your-ehr-domain.com
Access-Control-Allow-Methods: GET, POST, OPTIONS
Access-Control-Allow-Headers: Content-Type, Authorization
```

## Step 3: Frontend Configuration

### 3.1 Environment Variables
Create or update `.env.local` in your EHR UI directory:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
```

### 3.2 Component Integration
The following components have been created and integrated:

1. **VideoCallModal.tsx** - Main video call interface
2. **VideoCallButton.tsx** - Trigger button for video calls
3. **Updated Calendar.tsx** - Appointment scheduling with telehealth option
4. **Updated PatientComponents.tsx** - Patient dashboard with video call access

## Step 4: Database Setup (Optional)

If you want to track video call sessions, you can add a table:

```sql
CREATE TABLE video_call_sessions (
    id BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    provider_id BIGINT,
    room_name VARCHAR(255) NOT NULL,
    jwt_token TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    status VARCHAR(50) DEFAULT 'CREATED',
    FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    FOREIGN KEY (patient_id) REFERENCES patients(id),
    FOREIGN KEY (provider_id) REFERENCES providers(id)
);
```

## Step 5: Testing the Integration

### 5.1 Backend API Testing

**Test service registration**:
```bash
curl -X GET http://localhost:8080/api/telehealth/providers
```

**Test token generation**:
```bash
curl -X POST http://localhost:8080/api/telehealth/jitsi/join \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_AUTH_TOKEN" \
  -d '{
    "patientId": 1,
    "appointmentId": 123,
    "userType": "provider"
  }'
```

Expected response:
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "meetingUrl": "https://meet-stg.ciyex.com/appointment-123",
    "roomName": "appointment-123"
  }
}
```

### 5.2 Frontend Testing

1. **Start the backend**: `./gradlew bootRun`
2. **Start the frontend**: `npm run dev` (in ciyex-ehr-ui directory)
3. **Create telehealth appointment**:
   - Go to calendar
   - Create new appointment
   - Select "Telehealth" as visit type
4. **Test video call**:
   - View appointment in patient dashboard
   - Click "Join Video Call" button
   - Verify modal opens and token is generated

## Step 6: Production Deployment

### 6.1 Security Considerations

1. **JWT Secret**: Use a strong, unique secret key
2. **HTTPS**: Ensure all communication is over HTTPS
3. **Token Expiration**: Set appropriate token expiration times
4. **Rate Limiting**: Implement rate limiting for token generation

### 6.2 Monitoring

Add monitoring for:
- Token generation failures
- Video call session durations
- API response times
- Jitsi server health

### 6.3 Backup Configuration

Store configuration in version control:
```yaml
# config/telehealth-production.yml
telehealth:
  providers:
    jitsi:
      serverUrl: "${JITSI_SERVER_URL:https://meet-stg.ciyex.com}"
      appId: "${JITSI_APP_ID}"
      appSecret: "${JITSI_APP_SECRET}"
      tokenExpirationMinutes: "${JITSI_TOKEN_EXPIRATION:60}"
```

## Troubleshooting

### Common Issues

**Issue**: "No telehealth provider found for vendor: jitsi"
**Solution**: Ensure `@TelehealthVendor("jitsi")` annotation is present and component scan includes the service package.

**Issue**: "Invalid JWT token"
**Solution**: Verify JWT secret matches between backend and Jitsi server configuration.

**Issue**: "Room not found"
**Solution**: Check room naming convention and ensure room is created before patient attempts to join.

**Issue**: CORS errors in browser
**Solution**: Configure Jitsi server to allow requests from your EHR domain.

### Debug Logging

Enable debug logging in `application.yml`:
```yaml
logging:
  level:
    com.ciyex.telehealth: DEBUG
    org.springframework.web: DEBUG
```

### Health Check Endpoints

Add health check for Jitsi connectivity:
```java
@RestController
@RequestMapping("/api/health")
public class HealthController {
    
    @GetMapping("/jitsi")
    public ResponseEntity<Map<String, Object>> checkJitsiHealth() {
        // Implement Jitsi server connectivity check
    }
}
```

## Support

For additional support:
1. Check Jitsi Meet documentation: https://jitsi.github.io/handbook/
2. Review JWT.io for token debugging: https://jwt.io/
3. Check application logs for detailed error messages
4. Verify network connectivity between all components