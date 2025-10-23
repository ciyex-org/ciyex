# Ciyex Run Configurations Guide

## Overview

This project includes comprehensive VS Code launch configurations and standalone scripts for running all Ciyex services.

## 🚀 VS Code Launch Configurations

### Individual Services

#### 1. **Ciyex Backend (Auto Build)**
- **Port**: 8080
- **Pre-launch**: Compiles code, checks port, kills existing process, builds
- **Profile**: `local`
- **Usage**: Press `F5` and select this configuration

#### 2. **EHR UI (Next.js)**
- **Port**: 3000
- **Pre-launch**: Checks port, kills existing process
- **Auto-opens**: Browser when ready
- **Usage**: Press `F5` and select this configuration

#### 3. **Portal UI (Next.js)**
- **Port**: 3001
- **Pre-launch**: Checks port, kills existing process
- **Auto-opens**: Browser when ready
- **Usage**: Press `F5` and select this configuration

### Compound Configurations (Run Multiple Services)

#### 1. **Full Stack (Backend + EHR UI)**
Starts both Backend and EHR UI together
- Backend on port 8080
- EHR UI on port 3000

#### 2. **Full Stack (Backend + Portal UI)**
Starts both Backend and Portal UI together
- Backend on port 8080
- Portal UI on port 3001

#### 3. **All Services (Backend + Both UIs)**
Starts all three services together
- Backend on port 8080
- EHR UI on port 3000
- Portal UI on port 3001

## 📜 Standalone Scripts

### Individual Service Scripts

#### Start Backend
```bash
./start-backend.sh
```
- Compiles Java code
- Checks and frees port 8080
- Builds application
- Starts backend server

#### Start EHR UI
```bash
./start-ehr-ui.sh
```
- Checks and frees port 3000
- Installs dependencies if needed
- Starts EHR UI development server

#### Start Portal UI
```bash
./start-portal-ui.sh
```
- Checks and frees port 3001
- Installs dependencies if needed
- Starts Portal UI development server

### All Services Script

#### Start All Services
```bash
./start-all.sh
```
- Starts Backend, EHR UI, and Portal UI
- Runs all services in background
- Monitors logs in real-time
- Press `Ctrl+C` to stop all services

**Output:**
```
Services:
  Backend:   http://localhost:8080 (PID: 12345)
  EHR UI:    http://localhost:3000 (PID: 12346)
  Portal UI: http://localhost:3001 (PID: 12347)

Logs:
  Backend:   tail -f backend.log
  EHR UI:    tail -f ehr-ui.log
  Portal UI: tail -f portal-ui.log
```

## 🛠️ VS Code Tasks

Available tasks (Run via `Ctrl+Shift+P` → "Tasks: Run Task"):

### Build Tasks
- **Gradle Build** - Build backend with tests
- **Build and Check Port Backend** - Pre-launch task for backend
- **Install Dependencies** - Install npm dependencies for UIs

### Port Management Tasks
- **Kill Port 8080** - Free backend port
- **Kill Port 3000** - Free EHR UI port
- **Kill Port 3001** - Free Portal UI port
- **Kill All Ports** - Free all service ports

### Port Check Tasks
- **Check Port 3000** - Check and free EHR UI port
- **Check Port 3001** - Check and free Portal UI port

## 📁 Helper Scripts (in `scripts/` directory)

### `start-backend-check.sh`
Pre-launch script for backend:
1. Compiles Java code
2. Checks port 8080
3. Kills existing process if needed
4. Builds application

### `check-port.sh`
Generic port checker:
```bash
./scripts/check-port.sh <port> <service-name>
```

### `install-deps.sh`
Installs npm dependencies for both UIs if needed

## 🎯 Quick Start Guide

### Option 1: VS Code (Recommended)

**Run Single Service:**
1. Press `F5`
2. Select service from dropdown
3. Service starts automatically

**Run Multiple Services:**
1. Press `F5`
2. Select compound configuration:
   - "Full Stack (Backend + EHR UI)"
   - "Full Stack (Backend + Portal UI)"
   - "All Services (Backend + Both UIs)"
3. All services start together

### Option 2: Terminal

**Run Single Service:**
```bash
# Backend only
./start-backend.sh

# EHR UI only
./start-ehr-ui.sh

# Portal UI only
./start-portal-ui.sh
```

**Run All Services:**
```bash
./start-all.sh
```

## 🔧 Troubleshooting

### Port Already in Use
Run the kill task:
```bash
# Kill specific port
lsof -ti:8080 | xargs kill -9

# Or use VS Code task
Ctrl+Shift+P → "Tasks: Run Task" → "Kill Port 8080"
```

### Backend Won't Start (ClassNotFoundException)
The VS Code configuration uses Gradle's classpath, which fixes this issue. If you still encounter it:
```bash
./gradlew clean build --refresh-dependencies
```

### Dependencies Not Installed
```bash
# EHR UI
cd ciyex-ehr-ui && npm install

# Portal UI
cd ciyex-patient-portal && npm install

# Or use script
./scripts/install-deps.sh
```

### Build Fails
```bash
# Clean build
./gradlew clean build -x test

# Refresh dependencies
./gradlew clean build --refresh-dependencies
```

## 📊 Service URLs

| Service | URL | Port |
|---------|-----|------|
| Backend API | http://localhost:8080 | 8080 |
| Backend Health | http://localhost:8080/actuator/health | 8080 |
| EHR UI | http://localhost:3000 | 3000 |
| Portal UI | http://localhost:3001 | 3001 |

## 🔐 Environment Variables

### Backend
Set in `application.yml`:
- `SPRING_PROFILES_ACTIVE=local`
- `KEYCLOAK_CLIENT_SECRET=LgDl38nUg6leUfB9DCG5LDglC75bxpOp`

### EHR UI
Set in `ciyex-ehr-ui/.env.local`:
- `NEXT_PUBLIC_API_URL=http://localhost:8080`
- `NEXT_PUBLIC_KEYCLOAK_ENABLED=true`
- `NEXT_PUBLIC_KEYCLOAK_URL=https://aran-stg.zpoa.com`
- `NEXT_PUBLIC_KEYCLOAK_REALM=master`
- `NEXT_PUBLIC_KEYCLOAK_CLIENT_ID=ciyex-app`

### Portal UI
Set in `ciyex-patient-portal/.env.local` (if exists)

## 📝 Log Files

When using `start-all.sh`, logs are written to:
- `backend.log` - Backend server logs
- `ehr-ui.log` - EHR UI logs
- `portal-ui.log` - Portal UI logs

View logs in real-time:
```bash
tail -f backend.log
tail -f ehr-ui.log
tail -f portal-ui.log
```

## 🎉 Benefits

✅ **One-click launch** - Start services with F5 in VS Code
✅ **Automatic port management** - No more "port already in use" errors
✅ **Automatic compilation** - Always runs latest code
✅ **Dependency checking** - Installs npm packages if needed
✅ **Multi-service launch** - Start entire stack with one command
✅ **Clean builds** - Ensures fresh compilation every time
✅ **Browser auto-open** - UIs open automatically when ready

## 🆘 Support

If you encounter issues:
1. Check the troubleshooting section above
2. Review log files for errors
3. Ensure all dependencies are installed
4. Try a clean build: `./gradlew clean build`
5. Restart VS Code to refresh configurations

---

**Last Updated**: October 22, 2025
**Version**: 1.0.0
