# Frontend Practice Selection Implementation Guide

## Overview

When a user belongs to multiple tenant groups, the EHR UI should show a practice selection page and store the selected practice in localStorage. All subsequent API requests must include the `X-Tenant-Name` header.

## Backend API

### GET /api/tenants/accessible

Returns the list of tenants the user can access.

**Request:**
```http
GET /api/tenants/accessible
Authorization: Bearer <jwt-token>
```

**Response:**
```json
{
  "success": true,
  "message": "Accessible tenants retrieved",
  "data": {
    "hasFullAccess": false,
    "tenants": ["Qiaben Health", "MediPlus", "CareWell"],
    "requiresSelection": true
  }
}
```

**Response Fields:**
- `hasFullAccess`: `true` if user has Apps group access (can access any tenant)
- `tenants`: List of accessible tenant names (empty array if `hasFullAccess` is true)
- `requiresSelection`: `true` if user needs to select a practice

## Frontend Implementation

### 1. Check on Login/App Load

```typescript
// src/services/tenantService.ts
import axios from 'axios';

export interface AccessibleTenantsResponse {
  hasFullAccess: boolean;
  tenants: string[];
  requiresSelection: boolean;
}

export const getAccessibleTenants = async (token: string): Promise<AccessibleTenantsResponse> => {
  const response = await axios.get('/api/tenants/accessible', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  return response.data.data;
};

export const getSelectedTenant = (): string | null => {
  return localStorage.getItem('selectedTenant');
};

export const setSelectedTenant = (tenantName: string): void => {
  localStorage.setItem('selectedTenant', tenantName);
};

export const clearSelectedTenant = (): void => {
  localStorage.removeItem('selectedTenant');
};
```

### 2. Practice Selection Component

```typescript
// src/components/PracticeSelection.tsx
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getAccessibleTenants, setSelectedTenant } from '../services/tenantService';

interface PracticeSelectionProps {
  token: string;
}

export const PracticeSelection: React.FC<PracticeSelectionProps> = ({ token }) => {
  const [tenants, setTenants] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    loadTenants();
  }, []);

  const loadTenants = async () => {
    try {
      const data = await getAccessibleTenants(token);
      
      if (!data.requiresSelection) {
        // Single tenant, auto-select and proceed
        if (data.tenants.length === 1) {
          setSelectedTenant(data.tenants[0]);
          navigate('/dashboard');
        }
        return;
      }
      
      setTenants(data.tenants);
      setLoading(false);
    } catch (err) {
      setError('Failed to load practices');
      setLoading(false);
    }
  };

  const handleSelectPractice = (tenantName: string) => {
    setSelectedTenant(tenantName);
    navigate('/dashboard');
  };

  if (loading) {
    return <div>Loading practices...</div>;
  }

  if (error) {
    return <div className="error">{error}</div>;
  }

  return (
    <div className="practice-selection-container">
      <h1>Select Practice</h1>
      <p>You have access to multiple practices. Please select one to continue.</p>
      
      <div className="practice-list">
        {tenants.map((tenant) => (
          <div 
            key={tenant}
            className="practice-card"
            onClick={() => handleSelectPractice(tenant)}
          >
            <h3>{tenant}</h3>
            <button>Select</button>
          </div>
        ))}
      </div>
    </div>
  );
};
```

### 3. Axios Interceptor for X-Tenant-Name Header

```typescript
// src/config/axios.ts
import axios from 'axios';
import { getSelectedTenant } from '../services/tenantService';

// Create axios instance
export const api = axios.create({
  baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8080',
});

// Request interceptor to add X-Tenant-Name header
api.interceptors.request.use(
  (config) => {
    // Add Authorization header
    const token = localStorage.getItem('authToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    // Add X-Tenant-Name header
    const selectedTenant = getSelectedTenant();
    if (selectedTenant) {
      config.headers['X-Tenant-Name'] = selectedTenant;
    }
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor to handle errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 400 && 
        error.response?.data?.error === 'X-Tenant-Name header is required') {
      // Redirect to practice selection
      window.location.href = '/select-practice';
    }
    return Promise.reject(error);
  }
);

export default api;
```

### 4. App Router with Practice Selection

```typescript
// src/App.tsx
import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Login } from './pages/Login';
import { PracticeSelection } from './components/PracticeSelection';
import { Dashboard } from './pages/Dashboard';
import { ProtectedRoute } from './components/ProtectedRoute';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        
        <Route 
          path="/select-practice" 
          element={
            <ProtectedRoute>
              <PracticeSelection token={localStorage.getItem('authToken') || ''} />
            </ProtectedRoute>
          } 
        />
        
        <Route 
          path="/dashboard" 
          element={
            <ProtectedRoute requireTenant>
              <Dashboard />
            </ProtectedRoute>
          } 
        />
        
        <Route path="/" element={<Navigate to="/dashboard" />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
```

### 5. Protected Route Component

```typescript
// src/components/ProtectedRoute.tsx
import React, { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { getAccessibleTenants, getSelectedTenant } from '../services/tenantService';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requireTenant?: boolean;
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ 
  children, 
  requireTenant = false 
}) => {
  const [loading, setLoading] = useState(true);
  const [needsSelection, setNeedsSelection] = useState(false);
  const token = localStorage.getItem('authToken');

  useEffect(() => {
    checkAccess();
  }, []);

  const checkAccess = async () => {
    if (!token) {
      setLoading(false);
      return;
    }

    if (requireTenant) {
      const selectedTenant = getSelectedTenant();
      
      if (!selectedTenant) {
        // Check if user needs to select a practice
        try {
          const data = await getAccessibleTenants(token);
          
          if (data.requiresSelection) {
            setNeedsSelection(true);
          } else if (data.tenants.length === 1) {
            // Auto-select single tenant
            localStorage.setItem('selectedTenant', data.tenants[0]);
          }
        } catch (err) {
          console.error('Failed to check tenant access', err);
        }
      }
    }
    
    setLoading(false);
  };

  if (loading) {
    return <div>Loading...</div>;
  }

  if (!token) {
    return <Navigate to="/login" />;
  }

  if (needsSelection) {
    return <Navigate to="/select-practice" />;
  }

  return <>{children}</>;
};
```

### 6. Practice Switcher Component (Optional)

```typescript
// src/components/PracticeSwitcher.tsx
import React, { useState, useEffect } from 'react';
import { getAccessibleTenants, getSelectedTenant, setSelectedTenant } from '../services/tenantService';

export const PracticeSwitcher: React.FC = () => {
  const [tenants, setTenants] = useState<string[]>([]);
  const [selectedTenant, setSelected] = useState<string | null>(getSelectedTenant());
  const [showDropdown, setShowDropdown] = useState(false);

  useEffect(() => {
    loadTenants();
  }, []);

  const loadTenants = async () => {
    const token = localStorage.getItem('authToken');
    if (!token) return;

    try {
      const data = await getAccessibleTenants(token);
      setTenants(data.tenants);
    } catch (err) {
      console.error('Failed to load tenants', err);
    }
  };

  const handleSwitch = (tenantName: string) => {
    setSelectedTenant(tenantName);
    setSelected(tenantName);
    setShowDropdown(false);
    // Reload the page to apply new tenant context
    window.location.reload();
  };

  if (tenants.length <= 1) {
    return null; // Don't show switcher for single tenant users
  }

  return (
    <div className="practice-switcher">
      <button onClick={() => setShowDropdown(!showDropdown)}>
        {selectedTenant || 'Select Practice'} ▼
      </button>
      
      {showDropdown && (
        <div className="dropdown">
          {tenants.map((tenant) => (
            <div
              key={tenant}
              className={`dropdown-item ${tenant === selectedTenant ? 'active' : ''}`}
              onClick={() => handleSwitch(tenant)}
            >
              {tenant}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
```

## Flow Diagram

```
User Login
    ↓
Get Accessible Tenants
    ↓
    ├─ Single Tenant?
    │   ↓ Yes
    │   Auto-select → Store in localStorage → Dashboard
    │
    └─ Multiple Tenants?
        ↓ Yes
        Show Practice Selection Page
            ↓
        User Selects Practice
            ↓
        Store in localStorage
            ↓
        Dashboard (all requests include X-Tenant-Name)
```

## API Request Example

```typescript
// Using the configured axios instance
import api from './config/axios';

// All requests automatically include X-Tenant-Name header
const getPatients = async () => {
  const response = await api.get('/api/patients');
  return response.data;
};

// Manual request with fetch
const getPatients = async () => {
  const token = localStorage.getItem('authToken');
  const tenant = localStorage.getItem('selectedTenant');
  
  const response = await fetch('/api/patients', {
    headers: {
      'Authorization': `Bearer ${token}`,
      'X-Tenant-Name': tenant
    }
  });
  
  return response.json();
};
```

## Styling Example (CSS)

```css
/* src/styles/PracticeSelection.css */
.practice-selection-container {
  max-width: 800px;
  margin: 50px auto;
  padding: 20px;
}

.practice-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 20px;
  margin-top: 30px;
}

.practice-card {
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s;
}

.practice-card:hover {
  border-color: #2196F3;
  box-shadow: 0 4px 8px rgba(0,0,0,0.1);
  transform: translateY(-2px);
}

.practice-card h3 {
  margin: 0 0 15px 0;
  color: #333;
}

.practice-card button {
  width: 100%;
  padding: 10px;
  background: #2196F3;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
}

.practice-card button:hover {
  background: #1976D2;
}
```

## Testing

### Test Scenarios

1. **Single Tenant User:**
   - Should auto-select tenant
   - Should redirect to dashboard
   - Should not show practice selection

2. **Multi-Tenant User:**
   - Should show practice selection page
   - Should store selection in localStorage
   - Should include X-Tenant-Name in all requests

3. **Full Access User:**
   - Should show practice selection
   - Should be able to switch between any practice

4. **Missing X-Tenant-Name:**
   - Should receive 400 error
   - Should redirect to practice selection

## Summary

✅ Backend endpoint: `GET /api/tenants/accessible`  
✅ Store selected practice in localStorage  
✅ Axios interceptor adds `X-Tenant-Name` header automatically  
✅ Practice selection page for multi-tenant users  
✅ Auto-select for single-tenant users  
✅ Optional practice switcher in header
