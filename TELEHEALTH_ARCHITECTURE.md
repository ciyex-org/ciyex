# 🏥 CIYEX Telehealth System - Complete Architecture & Flow

## 📋 Table of Contents
1. [System Overview](#system-overview)
2. [Architecture Diagram](#architecture-diagram)
3. [Backend API Endpoints](#backend-api-endpoints)
4. [Frontend Components](#frontend-components)
5. [Data Flow](#data-flow)
6. [Appointment Booking Flow](#appointment-booking-flow)
7. [Telehealth Video Call Flow](#telehealth-video-call-flow)
8. [Key Features](#key-features)
9. [Troubleshooting](#troubleshooting)

---

## 🎯 System Overview

The CIYEX Telehealth System is a comprehensive healthcare platform that enables:
- **Appointment Management**: Patients can book, view, and manage appointments
- **Telehealth Video Calls**: Secure video consultations using Jitsi Meet
- **Multi-tenant Support**: Each healthcare organization has isolated data
- **Provider Management**: Healthcare providers can manage their schedules
- **Real-time Notifications**: Patients are notified when video rooms are ready

---

## 🏗️ Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                       PATIENT PORTAL (Next.js)                   │
│  ┌────────────────┐  ┌────────────────┐  ┌───────────────────┐ │
│  │  Appointments  │  │   Telehealth   │  │  Authentication   │ │
│  │     Page       │  │     Page       │  │    (JWT)          │ │
│  └────────┬───────┘  └────────┬───────┘  └─────────┬─────────┘ │
│           │                   │                     │            │
└───────────┼───────────────────┼─────────────────────┼────────────┘
            │                   │                     │
            │ HTTP/JSON         │ WebSocket/REST      │ JWT Token
            ▼                   ▼                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                    SPRING BOOT BACKEND (Java)                    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                      Controllers                          │   │
│  │  ┌────────────────┐  ┌─────────────────┐  ┌───────────┐ │   │
│  │  │ Portal         │  │  Telehealth     │  │  Auth     │ │   │
│  │  │ Appointment    │  │  Controller     │  │  Filter   │ │   │
│  │  │ Controller     │  │                 │  │           │ │   │
│  │  └───────┬────────┘  └────────┬────────┘  └─────┬─────┘ │   │
│  └──────────┼─────────────────────┼──────────────────┼───────┘   │
│             │                     │                  │            │
│             ▼                     ▼                  ▼            │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                         Services                          │   │
│  │  ┌────────────────┐  ┌─────────────────┐  ┌───────────┐ │   │
│  │  │ Appointment    │  │  Jitsi          │  │  JWT      │ │   │
│  │  │ Service        │  │  Telehealth     │  │  Token    │ │   │
│  │  │                │  │  Service        │  │  Util     │ │   │
│  │  └───────┬────────┘  └────────┬────────┘  └─────┬─────┘ │   │
│  └──────────┼─────────────────────┼──────────────────┼───────┘   │
│             │                     │                  │            │
│             ▼                     ▼                  ▼            │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    Repositories & Entities                │   │
│  │  ┌────────────┐  ┌────────────┐  ┌──────────────────┐   │   │
│  │  │Appointment │  │  Provider  │  │  Organization    │   │   │
│  │  │  Entity    │  │  Entity    │  │  Config Entity   │   │   │
│  │  └─────┬──────┘  └─────┬──────┘  └────────┬─────────┘   │   │
│  └────────┼───────────────┼──────────────────┼─────────────┘   │
└───────────┼───────────────┼──────────────────┼─────────────────┘
            │               │                  │
            ▼               ▼                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                     PostgreSQL Database                          │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌──────────┐  │
│  │Appointments│  │ Providers  │  │ Locations  │  │ Patients │  │
│  └────────────┘  └────────────┘  └────────────┘  └──────────┘  │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌──────────┐  │
│  │Org Configs │  │ List       │  │ Portal     │  │ Audit    │  │
│  │            │  │ Options    │  │ Users      │  │ Logs     │  │
│  └────────────┘  └────────────┘  └────────────┘  └──────────┘  │
└─────────────────────────────────────────────────────────────────┘
            │
            ▼
┌─────────────────────────────────────────────────────────────────┐
│                     JITSI MEET SERVER                            │
│              (Video Conferencing Infrastructure)                 │
│  • JWT Authentication                                            │
│  • Room Management                                               │
│  • Video/Audio Streaming                                         │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔌 Backend API Endpoints

### **1. Portal Appointment Endpoints**

#### GET `/api/portal/appointments`
**Purpose**: Get all appointments for the logged-in patient

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
X-Org-Id: <ORGANIZATION_ID>
```

**Response**:
```json
{
  "success": true,
  "message": "Appointments retrieved successfully",
  "data": [
    {
      "id": 123,
      "visitType": "Telehealth",
      "providerId": 45,
      "providerName": "Dr. John Smith",
      "locationId": 10,
      "locationName": "Main Clinic",
      "appointmentStartDate": "2025-10-15",
      "appointmentStartTime": "10:00:00",
      "formattedDate": "Tue, Oct 15, 2025",
      "formattedTime": "10:00 AM",
      "status": "SCHEDULED",
      "priority": "Routine",
      "reason": "Follow-up consultation"
    }
  ]
}
```

---

#### GET `/api/portal/appointments/available-slots`
**Purpose**: Get available time slots for a provider on a specific date

**Query Parameters**:
- `provider_id`: Provider ID (optional, if not provided returns slots for all providers)
- `location_id`: Location ID (optional)
- `date`: Date in MM/dd/yy format (e.g., "10/15/25")
- `limit`: Max number of slots to return (default: 3)

**Response**:
```json
{
  "success": true,
  "message": "Available slots retrieved",
  "data": [
    {
      "id": 45001,
      "orgId": 1,
      "providerId": 45,
      "start": "2025-10-15T09:00:00",
      "end": "2025-10-15T10:00:00",
      "status": "free",
      "audit": {
        "createdDate": "2025-10-08T10:00:00",
        "lastModifiedDate": "2025-10-08T10:00:00"
      }
    }
  ]
}
```

---

#### POST `/api/portal/appointments`
**Purpose**: Create a new appointment request

**Request Body**:
```json
{
  "visitType": "Telehealth",
  "providerId": 45,
  "locationId": 10,
  "date": "10/15/25",
  "time": "10:00:00",
  "reason": "Follow-up consultation",
  "priority": "Routine"
}
```

**Response**:
```json
{
  "success": true,
  "message": "Appointment request submitted successfully. You will be notified once it's approved.",
  "data": {
    "id": 124,
    "status": "PENDING",
    ...
  }
}
```

---

### **2. Provider Endpoints**

#### GET `/api/portal/providers`
**Purpose**: Get list of all providers

**Response**:
```json
{
  "success": true,
  "data": [
    {
      "id": 45,
      "identification": {
        "firstName": "John",
        "lastName": "Smith"
      },
      "professionalDetails": {
        "specialty": "Cardiology"
      }
    }
  ]
}
```

---

### **3. Location Endpoints**

#### GET `/api/portal/locations`
**Purpose**: Get list of all locations

**Response**:
```json
{
  "success": true,
  "data": [
    {
      "id": 10,
      "name": "Main Clinic",
      "address": "123 Healthcare Ave, City, State 12345"
    }
  ]
}
```

---

### **4. List Options Endpoints**

#### GET `/api/portal/list-options?list_id={listId}`
**Purpose**: Get master list options (visit types, priorities, etc.)

**Query Parameters**:
- `list_id`: One of `visit_types`, `appointment_priorities`

**Response**:
```json
{
  "success": true,
  "data": [
    {
      "optionId": "telehealth",
      "title": "Telehealth",
      "notes": "Virtual video consultation"
    },
    {
      "optionId": "in-person",
      "title": "In-Person",
      "notes": "Office visit"
    }
  ]
}
```

---

### **5. Telehealth Endpoints**

#### POST `/api/telehealth/rooms`
**Purpose**: Create a new video room (typically called by provider)

**Request Body**:
```json
{
  "providerId": 45,
  "patientId": 89,
  "roomName": "apt123"
}
```

**Response**:
```json
{
  "roomSid": "apt123"
}
```

---

#### POST `/api/telehealth/jitsi/join`
**Purpose**: Generate Jitsi JWT token and meeting URL to join a video room

**Request Body**:
```json
{
  "roomName": "apt123",
  "identity": "patient-89",
  "ttlSeconds": 3600
}
```

**Response**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "roomName": "apt123",
  "identity": "patient-89",
  "meetingUrl": "https://meet.ciyex.com/apt123?jwt=eyJhbGc...",
  "expiresIn": 3600
}
```

**Error Response (Room not ready)**:
```json
{
  "error": "Room not found or not started yet"
}
```

---

## 🖥️ Frontend Components

### **1. Appointments Page** (`/appointments/page.tsx`)

**Location**: `ciyex-portal-ui/src/app/appointments/page.tsx`

**Key Features**:
- Lists all patient appointments (past, upcoming, telehealth)
- Shows appointment details (date, time, provider, location, status)
- "Request Appointment" button opens modal
- Real-time polling to check if telehealth rooms are ready
- "Join Video Call" button when provider starts room
- Responsive table with status badges

**Key State**:
```tsx
const [appointments, setAppointments] = useState<Appointment[]>([]);
const [providers, setProviders] = useState<Provider[]>([]);
const [locations, setLocations] = useState<Location[]>([]);
const [visitTypes, setVisitTypes] = useState<string[]>([]);
const [priorities, setPriorities] = useState<string[]>([]);
const [telehealthReady, setTelehealthReady] = useState<Record<number, { meetingUrl?: string }>>({});
```

**Key Functions**:
- `loadData()`: Fetches appointments, providers, locations on mount
- `fetchSlots(providerId, date)`: Gets available time slots
- `handleSubmit()`: Creates new appointment request
- `checkRooms()`: Polls for telehealth room readiness (every 15s)

---

### **2. Telehealth Page** (`/telehealth/[appointmentId]/page.tsx`)

**Location**: `ciyex-portal-ui/src/app/telehealth/[appointmentId]/page.tsx`

**Key Features**:
- Loads appointment details
- Generates Jitsi JWT token
- Embeds video call in iframe
- "End Call" button to leave
- Shows connection status and instructions

**Key Functions**:
- `initVideoCall()`: Joins video room using Jitsi JWT
- Uses `getTelehealthIdentity()` to get patient identity from JWT token

---

### **3. JWT Helper Utility** (`/utils/jwtHelper.ts`)

**Location**: `ciyex-portal-ui/src/utils/jwtHelper.ts`

**Key Functions**:
- `decodeJWT(token)`: Decodes JWT payload (client-side only, no verification)
- `getPatientIdFromToken()`: Extracts patient ID from stored token
- `getPatientEmailFromToken()`: Extracts email from token
- `getOrgIdsFromToken()`: Extracts organization IDs
- `getTelehealthIdentity()`: Generates identity string for telehealth (e.g., "patient-89")
- `isTokenExpired()`: Checks if token is expired

---

## 🔄 Data Flow

### **Complete Data Flow Diagram**

```
1. PATIENT LOGIN
   └─→ Store JWT token in localStorage (contains: patientId, orgIds, email)

2. LOAD APPOINTMENTS PAGE
   ├─→ GET /api/portal/appointments
   ├─→ GET /api/portal/providers
   ├─→ GET /api/portal/locations
   ├─→ GET /api/portal/list-options?list_id=visit_types
   └─→ GET /api/portal/list-options?list_id=appointment_priorities

3. REQUEST APPOINTMENT
   ├─→ User clicks "Request Appointment"
   ├─→ Modal opens with form
   ├─→ User selects provider → GET /api/portal/appointments/available-slots
   ├─→ User picks date/time/location/reason
   ├─→ User clicks "Request"
   └─→ POST /api/portal/appointments
       └─→ Backend creates appointment with status "PENDING"
           └─→ Appointment appears in list

4. PROVIDER APPROVES APPOINTMENT (EHR side)
   └─→ Status changes from "PENDING" → "SCHEDULED"

5. TELEHEALTH ROOM READINESS POLLING (for virtual appointments)
   ├─→ Every 15 seconds: POST /api/telehealth/jitsi/join
   │   └─→ If provider started room: Returns meetingUrl
   │   └─→ If not started: Returns error
   └─→ When ready: Show banner "Your provider is ready to join"

6. JOIN VIDEO CALL
   ├─→ Patient clicks "Join" button
   ├─→ Navigate to /telehealth/{appointmentId}
   ├─→ POST /api/telehealth/jitsi/join
   │   └─→ Returns JWT token and meetingUrl
   └─→ Embed Jitsi meeting in iframe
       └─→ Patient and provider in same video room
```

---

## 📅 Appointment Booking Flow

### **Step-by-Step Process**

```
┌─────────────────────────────────────────────────────────────────┐
│ STEP 1: Open Modal                                              │
│ ┌───────────────────────────────────────────────────────────┐  │
│ │ User clicks "Request Appointment" button                   │  │
│ │ → Modal opens with form                                    │  │
│ └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────────┐
│ STEP 2: Select Provider                                         │
│ ┌───────────────────────────────────────────────────────────┐  │
│ │ User selects provider from dropdown                        │  │
│ │ → Providers loaded on page mount: GET /api/portal/providers│ │
│ └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────────┐
│ STEP 3: Pick Date                                               │
│ ┌───────────────────────────────────────────────────────────┐  │
│ │ User selects date from next 30 days dropdown               │  │
│ │ → Date options auto-generated on frontend                  │  │
│ │ → Triggers: fetchSlots(providerId, date)                   │  │
│ └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────────┐
│ STEP 4: Fetch Available Slots                                   │
│ ┌───────────────────────────────────────────────────────────┐  │
│ │ Frontend: Convert date YYYY-MM-DD → MM/dd/yy               │  │
│ │ Call: GET /api/portal/appointments/available-slots         │  │
│ │       ?provider_id=45&date=10/15/25                        │  │
│ │ Backend: Returns slots (e.g., 9:00 AM, 10:00 AM, 2:00 PM) │  │
│ │ Frontend: Displays slots as clickable buttons              │  │
│ └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────────┐
│ STEP 5: Complete Form                                           │
│ ┌───────────────────────────────────────────────────────────┐  │
│ │ User fills in:                                             │  │
│ │ • Time slot (from available slots)                         │  │
│ │ • Location (dropdown)                                      │  │
│ │ • Reason for visit (textarea)                              │  │
│ │ • Visit type (In-Person / Telehealth)                      │  │
│ │ • Priority (Routine / Urgent)                              │  │
│ └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────────┐
│ STEP 6: Submit Appointment Request                              │
│ ┌───────────────────────────────────────────────────────────┐  │
│ │ User clicks "Request" button                               │  │
│ │ Frontend: Converts date to MM/dd/yy format                 │  │
│ │ POST /api/portal/appointments                              │  │
│ │ Body: {                                                    │  │
│ │   visitType: "Telehealth",                                 │  │
│ │   providerId: 45,                                          │  │
│ │   locationId: 10,                                          │  │
│ │   date: "10/15/25",                                        │  │
│ │   time: "10:00:00",                                        │  │
│ │   reason: "Follow-up",                                     │  │
│ │   priority: "Routine"                                      │  │
│ │ }                                                          │  │
│ └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────────┐
│ STEP 7: Backend Processing                                      │
│ ┌───────────────────────────────────────────────────────────┐  │
│ │ PortalAppointmentController receives request              │  │
│ │ → Extract orgId from JWT token                             │  │
│ │ → Parse date from MM/dd/yy format                          │  │
│ │ → Create AppointmentDTO                                    │  │
│ │ → Set status = "PENDING"                                   │  │
│ │ → Save to database in tenant schema                        │  │
│ │ → Return success response with appointment data            │  │
│ └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────────┐
│ STEP 8: UI Update                                               │
│ ┌───────────────────────────────────────────────────────────┐  │
│ │ Frontend receives success response                         │  │
│ │ → Add new appointment to local state                       │  │
│ │ → Close modal                                              │  │
│ │ → Show success alert                                       │  │
│ │ → New appointment appears in table with "PENDING" badge    │  │
│ └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🎥 Telehealth Video Call Flow

### **Complete Telehealth Flow**

```
┌─────────────────────────────────────────────────────────────────┐
│ PROVIDER SIDE (EHR UI)                                           │
└─────────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────────┐
│ STEP 1: Provider starts video session                           │
│ ┌───────────────────────────────────────────────────────────┐  │
│ │ Provider views appointments page                           │  │
│ │ → Sees scheduled telehealth appointment                    │  │
│ │ → Clicks "Start Video Call" button                         │  │
│ │ → Navigates to /telehealth/{appointmentId}                 │  │
│ └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────────┐
│ STEP 2: Create room                                             │
│ ┌───────────────────────────────────────────────────────────┐  │
│ │ POST /api/telehealth/rooms                                 │  │
│ │ Body: {                                                    │  │
│ │   providerId: 45,                                          │  │
│ │   patientId: 89,                                           │  │
│ │   roomName: "apt123"                                       │  │
│ │ }                                                          │  │
│ │ Backend: Creates room record (optional, depends on impl)   │  │
│ └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────────┐
│ STEP 3: Provider gets join token                                │
│ ┌───────────────────────────────────────────────────────────┐  │
│ │ POST /api/telehealth/jitsi/join                            │  │
│ │ Body: {                                                    │  │
│ │   roomName: "apt123",                                      │  │
│ │   identity: "provider-45",                                 │  │
│ │   ttlSeconds: 3600                                         │  │
│ │ }                                                          │  │
│ │ Backend: Generates Jitsi JWT with moderator role           │  │
│ │ Returns: {                                                 │  │
│ │   token: "eyJ...",                                         │  │
│ │   meetingUrl: "https://meet.ciyex.com/apt123?jwt=..."     │  │
│ │ }                                                          │  │
│ └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────────┐
│ STEP 4: Provider joins video call                               │
│ ┌───────────────────────────────────────────────────────────┐  │
│ │ Frontend embeds Jitsi meeting in iframe or Jitsi SDK      │  │
│ │ → Provider enters video room "apt123"                      │  │
│ │ → Shows SOAP notes panel on the right                      │  │
│ └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘

═══════════════════════════════════════════════════════════════════

┌─────────────────────────────────────────────────────────────────┐
│ PATIENT SIDE (PORTAL UI)                                         │
└─────────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────────┐
│ STEP 5: Patient portal polls for room readiness                 │
│ ┌───────────────────────────────────────────────────────────┐  │
│ │ Background polling runs every 15 seconds                   │  │
│ │ For each SCHEDULED telehealth appointment:                 │  │
│ │   POST /api/telehealth/jitsi/join                          │  │
│ │   Body: {                                                  │  │
│ │     roomName: "apt123",                                    │  │
│ │     identity: "patient-89",                                │  │
│ │     ttlSeconds: 3600                                       │  │
│ │   }                                                        │  │
│ │                                                            │  │
│ │ IF provider has started room:                              │  │
│ │   → Backend returns: { meetingUrl: "https://..." }         │  │
│ │   → Frontend stores in telehealthReady state               │  │
│ │   → Shows banner: "Your provider is ready to join"         │  │
│ │   → "Join" button becomes active                           │  │
│ │                                                            │  │
│ │ IF provider hasn't started:                                │  │
│ │   → Backend returns error                                  │  │
│ │   → Frontend removes from telehealthReady state            │  │
│ │   → No banner shown                                        │  │
│ └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────────┐
│ STEP 6: Patient joins video call                                │
│ ┌───────────────────────────────────────────────────────────┐  │
│ │ Patient clicks "Join" button                               │  │
│ │ → Navigates to /telehealth/123                             │  │
│ │ → POST /api/telehealth/jitsi/join (gets fresh token)      │  │
│ │ → Embeds meeting in iframe                                 │  │
│ │ → Patient enters same video room "apt123"                  │  │
│ └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────────┐
│ STEP 7: Video consultation                                      │
│ ┌───────────────────────────────────────────────────────────┐  │
│ │ Both provider and patient are in the same Jitsi room      │  │
│ │ → Video and audio streaming                                │  │
│ │ → Provider can take notes in SOAP panel                    │  │
│ │ → Patient can see/hear provider                            │  │
│ └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────────┐
│ STEP 8: End call                                                │
│ ┌───────────────────────────────────────────────────────────┐  │
│ │ Patient clicks "End Call"                                  │  │
│ │ → Navigate back to /appointments                           │  │
│ │ Provider clicks "End Session"                              │  │
│ │ → Save SOAP notes                                          │  │
│ │ → Update appointment status to "COMPLETED"                 │  │
│ └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## ✨ Key Features

### **1. Multi-Tenant Support**
- Each healthcare organization has isolated data
- `orgId` extracted from JWT token
- Database uses tenant-aware queries

### **2. JWT Authentication**
- JWT token stored in localStorage
- Token contains: `patientId`, `orgIds`, `email`
- Automatic token injection in all API calls via `fetchWithAuth()`

### **3. Real-Time Polling**
- Patient portal polls every 15 seconds to check if telehealth room is ready
- Clears ready state if provider leaves or room no longer exists

### **4. Date/Time Format Handling**
- Frontend sends dates in MM/dd/yy format to backend
- Frontend receives ISO dates from backend and formats for display
- Time slots converted to 12-hour format with AM/PM

### **5. Loading States**
- Submitting button shows spinner during appointment creation
- Slot fetching shows loading indicator
- Prevents double submissions

---

## 🔧 Troubleshooting

### **Issue 1: "No available slots" message**
**Possible Causes**:
- Date format mismatch (frontend sends YYYY-MM-DD, backend expects MM/dd/yy)
- Provider has no slots configured
- Date is in the past

**Solution**:
- ✅ Fixed: Frontend now converts date to MM/dd/yy before sending
- Check backend logs for date parsing errors
- Verify provider has availability configured

---

### **Issue 2: "Unable to join video call" error**
**Possible Causes**:
- Provider hasn't started the room yet
- JWT token expired
- Jitsi configuration missing in organization config
- Network/CORS issues

**Solution**:
- ✅ Wait for provider to start the session first
- ✅ Ensure JWT token is valid and not expired
- Check backend logs for Jitsi configuration errors
- Verify CORS settings allow portal domain

---

### **Issue 3: Appointments not showing in list**
**Possible Causes**:
- Wrong orgId in JWT token
- Database tenant context not set
- Patient has no appointments

**Solution**:
- Check JWT token contains correct `orgIds`
- Verify `RequestContext` is set in backend
- Check database for appointments in correct schema

---

### **Issue 4: Time slots not loading**
**Possible Causes**:
- API endpoint not configured
- Provider ID not selected
- Date not selected

**Solution**:
- ✅ Fixed: Updated endpoint to `/api/portal/appointments/available-slots`
- ✅ Added loading indicator during fetch
- Ensure both provider and date are selected

---

## 📝 Summary

This system provides a complete telehealth solution with:
- ✅ Appointment booking with provider availability
- ✅ Secure video consultations using Jitsi
- ✅ Multi-tenant support
- ✅ Real-time room readiness notifications
- ✅ JWT-based authentication
- ✅ Responsive UI with loading states
- ✅ Date/time format handling
- ✅ Error handling and user feedback

All critical bugs have been fixed, and the system is ready for testing.

---

**Next Steps**:
1. ✅ All code changes complete
2. 🧪 Run integration tests
3. 🧪 Test appointment booking flow
4. 🧪 Test telehealth video calls
5. 🚀 Deploy to staging environment
