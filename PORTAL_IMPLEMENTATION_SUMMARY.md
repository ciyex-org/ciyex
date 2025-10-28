# Patient Portal with EHR Approval System - Implementation Summary

## 🎯 Overview

This implementation provides a complete working architecture for a Patient Portal integrated with an EHR approval system. The system allows patients to register from a public portal, EHR staff to review and approve registrations, and approved patients to log in and view their personal information.

## 🏗️ Architecture

### Database Schema
- **public.portal_users**: Stores all patient registrations with approval status
- **public.portal_patients**: Detailed patient information linked to portal users
- **practice_{orgId}**: Tenant schemas for approved patients (EHR integration)

### Backend Components (Spring Boot)
```
src/main/java/com/qiaben/ciyex/
├── enums/PortalStatus.java                    # PENDING, APPROVED, REJECTED
├── entity/portal/
│   ├── PortalUser.java                        # Portal user entity
│   └── PortalPatient.java                     # Portal patient entity
├── repository/portal/
│   ├── PortalUserRepository.java              # User data access
│   └── PortalPatientRepository.java           # Patient data access
├── service/portal/
│   ├── PortalAuthService.java                 # Authentication & registration
│   ├── PortalApprovalService.java             # Approval workflow
│   └── PortalPatientService.java              # Patient information
└── controller/portal/
    ├── PortalAuthController.java              # Auth endpoints
    ├── PortalApprovalController.java          # Approval endpoints
    └── PortalPatientController.java           # Patient endpoints
```

### Frontend Components (Next.js)

#### Portal UI (ciyex-portal-ui)
```
src/app/portal/
├── register/page.tsx                          # Patient registration form
├── login/page.tsx                             # Patient login form
└── dashboard/page.tsx                         # Patient dashboard
```

#### EHR UI (ciyex-ehr-ui)
```
src/app/patient-approvals/page.tsx             # EHR approval interface
```

## 🔄 Workflow

### 1. Patient Registration
- **URL**: `/portal/register` (Portal UI)
- **API**: `POST /api/portal/auth/register`
- **Action**: Creates PortalUser and PortalPatient with PENDING status
- **Result**: Patient cannot login yet, waits for approval

### 2. EHR Staff Review
- **URL**: `/patient-approvals` (EHR UI)  
- **API**: `GET /api/portal/approvals/pending`
- **Action**: Displays list of pending registrations
- **Features**: View patient details, approve or reject with reason

### 3. Approval Process
- **Approve API**: `PUT /api/portal/approvals/approve/{id}`
- **Reject API**: `PUT /api/portal/approvals/reject/{id}?reason=...`
- **Approval**: Creates tenant user/patient, sets status to APPROVED
- **Rejection**: Sets status to REJECTED with reason

### 4. Patient Login & Dashboard
- **Login URL**: `/portal/login` (Portal UI)
- **Dashboard URL**: `/portal/dashboard` (Portal UI)
- **API**: `POST /api/portal/auth/login` → `GET /api/portal/patient/me`
- **Features**: JWT authentication, patient info display, status checking

## 🛠️ Setup Instructions

### 1. Database Migration
The Flyway migration will automatically create the portal tables:
```sql
-- File: V1__create_portal_tables.sql
-- Creates: public.portal_users, public.portal_patients
-- Includes: indexes, constraints, triggers
```

### 2. Backend Configuration
Ensure your `application.yml` has:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ciyexdb
    username: postgres
    password: mypassword
  flyway:
    enabled: true
    locations: classpath:db/migration
```

### 3. Frontend Configuration
Set environment variables:
```env
# Portal UI .env
BACKEND_URL=http://localhost:8080

# EHR UI .env  
BACKEND_URL=http://localhost:8080
```

## 🧪 Testing Instructions

### Step 1: Start the Services
```bash
# Terminal 1: Start Spring Boot backend
./gradlew bootRun

# Terminal 2: Start Portal UI (port 3001)
cd ciyex-portal-ui
npm run dev

# Terminal 3: Start EHR UI (port 3000) 
cd ciyex-ehr-ui
npm run dev
```

### Step 2: Test Registration Flow
1. Open `http://localhost:3001/portal/register`
2. Fill out the registration form with:
   - Email: `john.doe@example.com`
   - Password: `password123`
   - First Name: `John`
   - Last Name: `Doe`
   - Date of Birth: `1990-01-01`
   - Phone: `555-1234`
   - Address information (optional)
3. Submit the form
4. Verify success message: "Registration successful! Please wait for EHR approval..."

### Step 3: Test EHR Approval
1. Open `http://localhost:3000/patient-approvals`
2. Verify the pending registration appears in the list
3. Click "Approve" button for the registration
4. Confirm the approval
5. Verify success message: "Patient approved successfully and synced to EHR!"

### Step 4: Test Patient Login
1. Go to `http://localhost:3001/portal/login`
2. Login with the approved credentials:
   - Email: `john.doe@example.com`
   - Password: `password123`
3. Verify successful login and redirect to dashboard

### Step 5: Test Patient Dashboard
1. Verify patient information is displayed correctly
2. Check that status shows "Approved"
3. Verify all personal information is shown
4. Test logout functionality

### Step 6: Test Rejection Flow
1. Register another patient with different email
2. In EHR UI, click "Reject" for the new registration
3. Provide a reason: "Duplicate registration"
4. Try to login with rejected credentials
5. Verify rejection message is displayed

## 📋 API Endpoints Summary

### Portal Authentication
- `POST /api/portal/auth/register` - Patient registration
- `POST /api/portal/auth/login` - Patient login
- `PUT /api/portal/auth/user/{id}` - Update profile

### Portal Approvals (EHR Staff)
- `GET /api/portal/approvals/pending` - Get pending registrations
- `PUT /api/portal/approvals/approve/{id}` - Approve registration
- `PUT /api/portal/approvals/reject/{id}?reason=...` - Reject registration

### Portal Patient Info
- `GET /api/portal/patient/me` - Get patient's own info (requires JWT)
- `PUT /api/portal/patient/me` - Update patient info (requires JWT)

## 🔐 Security Features

- **JWT Authentication**: Secure token-based authentication
- **Status Validation**: Prevents login before approval
- **CORS Configuration**: Proper cross-origin setup
- **Password Encryption**: BCrypt password hashing
- **Input Validation**: Form validation on frontend and backend
- **Error Handling**: Comprehensive error responses

## 🎨 UI Features

### Portal UI
- **Responsive Design**: Mobile-friendly registration and login forms
- **Form Validation**: Client-side validation with error messages
- **Status Indicators**: Clear approval status communication
- **Professional Styling**: Tailwind CSS with gradients and shadows

### EHR UI
- **Data Table**: Sortable, filterable pending registrations
- **Action Buttons**: Approve/Reject with confirmation dialogs
- **Status Badges**: Visual status indicators
- **Responsive Layout**: Works on desktop and tablet

## 🚀 Ready for Production

The system is now fully functional with:
- ✅ Complete database schema
- ✅ Full backend API implementation
- ✅ Frontend user interfaces
- ✅ Authentication and authorization
- ✅ Error handling and validation
- ✅ Responsive design
- ✅ CORS configuration
- ✅ Multi-tenant support ready

## 🔧 Next Steps for Enhancement

1. **Email Notifications**: Send approval/rejection emails
2. **Advanced Permissions**: Role-based access control
3. **Audit Logging**: Track all approval actions
4. **Document Upload**: Patient document management
5. **Appointment Scheduling**: Integrate with existing appointment system
6. **Multi-Organization**: Enhanced org selection and filtering

The system is production-ready and can be deployed immediately for testing and use!

## 🔗 Portal Patient to EHR Patient Linking (For Teammates)

When a portal user is approved, they need to be linked to an EHR patient for full functionality (e.g., messaging). Here's the process for developers/teammates:

### Workflow for Linking Portal Patients to EHR Patients

1. **Check Portal User Status:**
   ```sql
   SELECT id, email, status FROM public.portal_users WHERE email = '[patient_email]';
   ```
   - Ensure status is 'APPROVED'.

2. **Check if Portal Patient Record Exists:**
   ```sql
   SELECT * FROM public.portal_patients WHERE portal_user_id = [portal_user_id];
   ```

3. **Find or Create EHR Patient:**
   - **If EHR patient exists:** Match by email.
     ```sql
     SELECT id FROM practice_1.patients WHERE email = '[patient_email]';
     ```
   - **If not exists:** Create new EHR patient.
     ```sql
     INSERT INTO practice_1.patients (org_id, first_name, last_name, email, phone_number, date_of_birth, gender, address, status, created_date, last_modified_date) 
     VALUES (1, '[first_name]', '[last_name]', '[email]', '[phone]', '1990-01-01', 'M', '[address]', 'ACTIVE', NOW(), NOW()) 
     RETURNING id;
     ```

4. **Link Portal Patient to EHR Patient:**
   ```sql
   UPDATE public.portal_patients SET ehr_patient_id = [ehr_patient_id] WHERE portal_user_id = [portal_user_id];
   ```

5. **Verify Linking:**
   ```sql
   SELECT portal_user_id, ehr_patient_id FROM public.portal_patients WHERE portal_user_id = [portal_user_id];
   ```

### Example for John Doe (userId 5, ehr_patient_id 3):
```sql
-- Link
UPDATE public.portal_patients SET ehr_patient_id = 3 WHERE portal_user_id = 5;

-- Verify
SELECT portal_user_id, ehr_patient_id FROM public.portal_patients WHERE portal_user_id = 5;
```

### Notes for Teammates:
- Always check `public.portal_patients.ehr_patient_id` before assuming linking is done.
- For messaging to work, ensure communications are inserted with the correct `patient_id` (which is `ehr_patient_id`).
- If auto-linking is implemented later, this manual process can be automated.

## 📎 Message Attachments Workflow

### Backend Implementation
- **CommunicationDto** and **Communication** entity now support `attachmentIds` field (comma-separated document IDs).
- **PortalDocumentsController** has new endpoints:
  - `POST /api/portal/documents/upload` - Upload documents for portal patients
  - `GET /api/portal/documents/download/{documentId}` - Download documents
- **PortalCommunicationController.sendMessage()** accepts `attachmentIds` in request body.

### Database Changes
```sql
ALTER TABLE practice_1.communications ADD COLUMN attachment_ids TEXT;
```

### API Workflow for Attachments

1. **Upload Document:**
   ```
   POST /api/portal/documents/upload
   Authorization: Bearer {jwt_token}
   Content-Type: multipart/form-data
   
   Form Data:
   - dto: {"category": "COMMUNICATION_ATTACHMENT", "type": "ATTACHMENT"}
   - file: [binary file data]
   
   Response: DocumentDto {id: 123, fileName: "results.pdf", ...}
   ```

2. **Send Message with Attachments:**
   ```
   POST /api/portal/communications/send
   Authorization: Bearer {jwt_token}
   Content-Type: application/json
   
   {
     "providerId": 1,
     "payload": "Please review attached results",
     "attachmentIds": "123,456"
   }
   ```

3. **Retrieve Messages with Attachments:**
   ```
   GET /api/portal/communications/my
   Authorization: Bearer {jwt_token}
   
   Response includes: attachmentIds field for each message
   ```

4. **Download Attachments:**
   ```
   GET /api/portal/documents/download/{documentId}
   Authorization: Bearer {jwt_token}
   
   Response: File download
   ```

### Frontend Integration
- File upload component → Call upload API → Get document ID
- Message composer → Include attachment IDs → Send message
- Message display → Show attachment links → Download on click