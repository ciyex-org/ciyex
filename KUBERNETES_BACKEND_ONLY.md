# Kubernetes Manifests - Backend Only Configuration

## Overview

Updated Kubernetes manifests to deploy **only the Spring Boot backend API** since EHR UI and Portal UI are now in separate repositories with their own deployments.

## Changes Made

### 1. Deployment (`ciyex-deployment-stage.yaml`)

**Removed:**
- UI container ports (3000, 3001, 3002)
- `NEXT_PUBLIC_API_URL` environment variable

**Kept:**
- Backend container port 8080
- Health check probes on `/actuator/health`
- Database configuration
- Spring Boot environment variables

### 2. Service (`ciyex-service-stage.yaml`)

**Removed:**
- `ehr-ui` port (3000)
- `portal-ui` port (3001)
- `admin-ui` port (3002)

**Kept:**
- `backend-api` port (8080)
- ClusterIP service type

### 3. Ingress (`ciyex-ingress-stage.yaml`)

**Removed:**
- `portal-stg.ciyex.com` host and routing
- `admin-stg.ciyex.com` host and routing
- UI-specific path routing

**Kept:**
- `stg.ciyex.com` host for backend API
- TLS certificate configuration
- SSL redirect annotation
- Large file upload support (1024m)

## New Architecture

### Before:
```
┌─────────────────────────────────────────┐
│ Ingress (stg.ciyex.com)                 │
├─────────────────────────────────────────┤
│ /api      → Backend (8080)              │
│ /         → EHR UI (3000)               │
│                                         │
│ portal-stg.ciyex.com → Portal UI (3001) │
│ admin-stg.ciyex.com  → Admin UI (3002)  │
└─────────────────────────────────────────┘
```

### After:
```
┌─────────────────────────────────────────┐
│ Ingress (stg.ciyex.com)                 │
├─────────────────────────────────────────┤
│ /         → Backend API (8080)          │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│ Separate UI Deployments (Other Repos)  │
├─────────────────────────────────────────┤
│ ehr-stg.ciyex.com    → EHR UI          │
│ portal-stg.ciyex.com → Portal UI       │
└─────────────────────────────────────────┘
```

## Deployment

The GitHub Actions workflow will automatically:
1. Build the backend Docker image
2. Push to Azure Container Registry
3. Apply these manifests to AKS staging

```bash
kubectl apply -f manifests/stage/
```

## Accessing the Backend

After deployment, the backend API will be available at:
- **Base URL**: `https://stg.ciyex.com`
- **API Endpoints**: `https://stg.ciyex.com/api/*`
- **Health Check**: `https://stg.ciyex.com/actuator/health`

## UI Deployments

Each UI should be deployed separately with its own:

### EHR UI
- **Repository**: `ciyex-ehr-ui`
- **Suggested Domain**: `ehr-stg.ciyex.com`
- **Backend API**: `https://stg.ciyex.com`

### Portal UI
- **Repository**: `ciyex-portal-ui`
- **Suggested Domain**: `portal-stg.ciyex.com`
- **Backend API**: `https://stg.ciyex.com`

### Example UI Ingress Configuration

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ciyex-ehr-ui-ingress
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  ingressClassName: webapprouting.kubernetes.azure.com
  tls:
    - hosts:
        - ehr-stg.ciyex.com
      secretName: ehr-ui-tls
  rules:
    - host: ehr-stg.ciyex.com
      http:
        paths:
          - pathType: Prefix
            path: /
            backend:
              service:
                name: ciyex-ehr-ui
                port:
                  number: 3000
```

## Environment Variables for UI

Each UI deployment should configure:

```yaml
env:
  - name: NEXT_PUBLIC_API_URL
    value: "https://stg.ciyex.com"
  - name: NEXT_PUBLIC_KEYCLOAK_URL
    value: "https://aran-stg.zpoa.com"
  - name: NEXT_PUBLIC_KEYCLOAK_REALM
    value: "master"
  - name: NEXT_PUBLIC_KEYCLOAK_CLIENT_ID
    value: "ciyex-app"
```

## CORS Configuration

Ensure the backend allows requests from UI domains. Update `application.yml`:

```yaml
cors:
  allowed-origins: 
    - https://ehr-stg.ciyex.com
    - https://portal-stg.ciyex.com
    - http://localhost:3000
    - http://localhost:3001
```

## Verification

After deployment, verify:

```bash
# Check deployment status
kubectl get deployment ciyex-app-stage -n default

# Check service
kubectl get service ciyex-app-stage -n default

# Check ingress
kubectl get ingress ciyex-ingress-stage -n default

# Check pods
kubectl get pods -l app=ciyex-app-stage -n default

# View logs
kubectl logs -l app=ciyex-app-stage -n default --tail=100

# Test health endpoint
curl https://stg.ciyex.com/actuator/health
```

## Rollback

If needed, rollback to previous deployment:

```bash
kubectl rollout undo deployment/ciyex-app-stage -n default
```

---

**Date**: October 28, 2024  
**Status**: ✅ Backend-only configuration ready
