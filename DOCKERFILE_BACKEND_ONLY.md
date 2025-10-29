# Dockerfile Updated for Backend-Only Deployment

## Changes Made

The Dockerfile has been updated to build and deploy **only the Spring Boot backend** since the EHR UI and Portal UI have been moved to separate repositories.

## What Was Changed

### 1. **Dockerfile** - Simplified to Backend Only

**Removed:**
- EHR UI build stage (Node.js/pnpm)
- Portal UI build stage (Node.js/npm)
- Admin UI build stage (commented out)
- Node.js installation in runtime
- UI directory copying
- Multi-port exposure (3000, 3001, 3002)
- `start.sh` script dependency

**Kept:**
- Spring Boot backend build (Gradle + JDK 21)
- Optimized multi-stage build with caching
- Minimal runtime image (openjdk:21-jdk-slim)

**New Structure:**
```dockerfile
# Stage 1: Build Spring Boot backend
FROM gradle:jdk21-ubi AS backend-builder
# ... gradle build with cache optimization

# Stage 2: Runtime
FROM openjdk:21-jdk-slim
# ... copy jar and run Spring Boot only
```

### 2. **.dockerignore** - Added UI Directories

Added explicit ignores for UI projects:
```
ciyex-ehr-ui
ciyex-portal-ui
ciyex-admin-ui
```

### 3. **.gitignore** - Added UI Directories

Added UI directories to prevent accidental commits:
```
ciyex-ehr-ui/
ciyex-portal-ui/
ciyex-admin-ui/
```

## Benefits

1. **Faster Builds**: No more Node.js dependencies or UI builds
2. **Smaller Image**: Reduced from ~2GB to ~500MB
3. **Simpler Deployment**: Single Java process, no orchestration needed
4. **Better Separation**: Each UI can be deployed independently
5. **Cleaner CI/CD**: Backend pipeline only handles backend code

## GitHub Actions

The existing workflow (`.github/workflows/build-and-deploy-stage.yml`) will now:
- ✅ Build only the Spring Boot backend
- ✅ Create a smaller Docker image
- ✅ Deploy faster to AKS
- ✅ No longer fail on missing UI directories

## Deployment Architecture

### Before:
```
┌─────────────────────────────────┐
│   Single Container              │
│  ┌──────────────────────────┐  │
│  │ Spring Boot (8080)       │  │
│  ├──────────────────────────┤  │
│  │ EHR UI (3000)            │  │
│  ├──────────────────────────┤  │
│  │ Portal UI (3001)         │  │
│  ├──────────────────────────┤  │
│  │ Admin UI (3002)          │  │
│  └──────────────────────────┘  │
└─────────────────────────────────┘
```

### After:
```
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ Backend Container│  │ EHR UI Container │  │Portal UI Container│
│                  │  │                  │  │                  │
│ Spring Boot      │  │ Next.js          │  │ Next.js          │
│ (8080)           │  │ (3000)           │  │ (3001)           │
└──────────────────┘  └──────────────────┘  └──────────────────┘
     (This repo)       (Separate repo)       (Separate repo)
```

## What to Do with UI Repositories

Each UI should have its own:

1. **Dockerfile** - Build Next.js app
   ```dockerfile
   FROM node:20 AS builder
   WORKDIR /app
   COPY package*.json ./
   RUN npm install
   COPY . .
   RUN npm run build
   
   FROM node:20-slim
   WORKDIR /app
   COPY --from=builder /app/.next ./.next
   COPY --from=builder /app/node_modules ./node_modules
   COPY --from=builder /app/package.json ./package.json
   EXPOSE 3000
   CMD ["npm", "start"]
   ```

2. **GitHub Actions** - Deploy independently
3. **Kubernetes Manifests** - Separate deployments

## Testing Locally

Build and run the backend only:

```bash
# Build the image
docker build -t ciyex-backend:latest .

# Run the container
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/ciyex \
  -e KEYCLOAK_AUTH_SERVER_URL=https://aran-stg.zpoa.com \
  ciyex-backend:latest
```

## Environment Variables

The backend still needs these environment variables:

```bash
# Database
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD

# Keycloak
KEYCLOAK_ENABLED=true
KEYCLOAK_AUTH_SERVER_URL
KEYCLOAK_REALM
KEYCLOAK_RESOURCE
KEYCLOAK_CLIENT_SECRET

# Schema
CIYEX_SCHEMA_NAME=public

# AWS S3 (if used)
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
AWS_REGION
AWS_S3_BUCKET_NAME
```

## Next Steps

1. ✅ Commit these changes to fix the GitHub Actions build
2. Create separate repositories for:
   - `ciyex-ehr-ui`
   - `ciyex-portal-ui`
3. Set up CI/CD for each UI repository
4. Update Kubernetes manifests to deploy all three services
5. Configure ingress/routing to connect frontend to backend

## Rollback

If you need to rollback to the monolithic approach, the `start.sh` script is still in the repository but not used. You can restore the previous Dockerfile from git history:

```bash
git log --all --full-history -- Dockerfile
git show <commit-hash>:Dockerfile > Dockerfile
```

---

**Date**: October 28, 2024  
**Status**: ✅ Ready for deployment
