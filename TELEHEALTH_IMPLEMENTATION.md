# Telehealth Implementation with Jitsi Integration

## Overview
This document outlines the complete implementation of telehealth functionality using Jitsi Meet deployed at https://meet-stg.ciyex.com/ integrated with the Java backend and EHR UI.

## Backend Implementation

### 1. JitsiTelehealthService.java
**Location**: `src/main/java/com/ciyex/telehealth/service/impl/JitsiTelehealthService.java`

**Key Features**:
- JWT token generation for secure room access
- Meeting room creation and management
- Provider and patient role-based access
- Integration with existing telehealth service interface

**Configuration Requirements**:
```yaml
telehealth:
  providers:
    jitsi:
      serverUrl: "https://meet-stg.ciyex.com"
      appId: "your-jitsi-app-id"
      appSecret: "your-jitsi-app-secret"
```

### 2. Enhanced TelehealthController.java
**New Endpoints**:
- `POST /api/telehealth/jitsi/join` - Generate Jitsi meeting token and URL
- Supports both provider and patient access patterns
- Returns JWT token and meeting URL for frontend integration

### 3. Updated Configuration
**TelehealthConfig.java**: Added Jitsi-specific configuration class
**TelehealthResolver.java**: Automatically detects and registers JitsiTelehealthService

## Frontend Implementation

### 1. VideoCallModal.tsx
**Location**: `ciyex-ehr-ui/src/components/telehealth/VideoCallModal.tsx`

**Features**:
- Provider interface for creating video call rooms
- Patient interface for joining existing rooms
- Copy patient join link functionality
- Integration with Jitsi API for room creation

### 2. VideoCallButton.tsx
**Location**: `ciyex-ehr-ui/src/components/telehealth/VideoCallButton.tsx`

**Features**:
- Reusable button component for initiating video calls
- Modal integration for video call management
- Configurable button variants and sizes

### 3. Enhanced Calendar.tsx
**Integration Points**:
- Added video call options in appointment modal
- Support for "Telehealth" visit type selection
- VideoCallButton integration for scheduled appointments

### 4. Enhanced PatientComponents.tsx
**Appointment Table Integration**:
- Added "Video Call" column to appointment table
- Shows video call button for scheduled telehealth appointments
- Conditional rendering based on appointment type and status

## Security Features

### JWT Token Authentication
- Secure token generation using HMAC SHA-256
- Role-based access (provider/patient)
- Room-specific tokens with expiration
- Meeting context embedded in token payload

### API Integration
- RESTful endpoints for token generation
- Secure communication between frontend and backend
- Error handling and validation

## Usage Workflow

### For Providers:
1. Create or view scheduled appointment in calendar
2. For telehealth appointments, click "Start Video Call" button
3. System generates secure room and patient join link
4. Provider can copy and share patient link
5. Provider joins meeting room directly

### For Patients:
1. View appointments in patient dashboard
2. For scheduled telehealth appointments, see "Join Video Call" button
3. Click button to join video meeting
4. System authenticates and connects to provider's room

## Configuration Setup

### 1. Backend Configuration
Add to `application.yml`:
```yaml
telehealth:
  providers:
    jitsi:
      serverUrl: "https://meet-stg.ciyex.com"
      appId: "your-jitsi-app-id"  # Obtain from Jitsi server
      appSecret: "your-secret-key"  # Secure secret for JWT signing
```

### 2. Frontend Environment
Ensure `NEXT_PUBLIC_API_URL` points to your backend API endpoint.

### 3. Jitsi Server Setup
Your Jitsi server at https://meet-stg.ciyex.com/ should be configured to accept JWT authentication.

## Testing the Implementation

### 1. Backend Testing
```bash
# Test Jitsi service registration
curl -X GET http://localhost:8080/api/telehealth/providers

# Test token generation
curl -X POST http://localhost:8080/api/telehealth/jitsi/join \
  -H "Content-Type: application/json" \
  -d '{"patientId": 1, "appointmentId": 123, "userType": "provider"}'
```

### 2. Frontend Testing
1. Create a telehealth appointment in the calendar
2. Navigate to appointment table
3. Verify video call button appears for telehealth appointments
4. Test modal functionality and token generation

## Integration Benefits

### Seamless Workflow
- Integrated directly into existing appointment management
- No additional login required for video calls
- Automatic room management and cleanup

### Security
- JWT-based authentication
- Role-based access control
- Secure token generation and validation

### User Experience
- One-click video call initiation
- Copy-paste patient links
- Responsive design for all devices

## Future Enhancements

### Potential Improvements:
1. **Call Recording**: Integrate with Jitsi recording features
2. **Chat Integration**: Pre-meeting and post-meeting chat
3. **Screen Sharing**: Enhanced collaboration tools
4. **Waiting Room**: Patient queue management
5. **Call Analytics**: Meeting duration and quality metrics

### Scalability Considerations:
1. **Load Balancing**: Multiple Jitsi server support
2. **Regional Servers**: Geographic distribution
3. **CDN Integration**: Optimized media delivery
4. **Auto-scaling**: Dynamic server provisioning

## Troubleshooting

### Common Issues:
1. **Token Generation Errors**: Check JWT secret configuration
2. **Room Access Issues**: Verify Jitsi server JWT settings
3. **Network Connectivity**: Ensure firewall allows Jitsi ports
4. **CORS Issues**: Configure Jitsi server for your domain

### Debugging:
- Enable debug logging for telehealth service
- Check browser developer tools for API errors
- Verify Jitsi server logs for authentication issues

## Conclusion

This implementation provides a complete, secure, and user-friendly telehealth solution integrated with your existing EHR system. The modular design allows for easy maintenance and future enhancements while maintaining the highest security standards for healthcare applications.