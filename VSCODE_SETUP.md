# VS Code Run Configurations Setup Guide

## 🎯 Quick Start

### Step 1: Install Configurations

Run the setup script to install VS Code configurations:

```bash
./.vscode-templates/setup-vscode.sh
```

This will copy `launch.json` and `tasks.json` to your `.vscode/` directory.

### Step 2: Reload VS Code

Press `Ctrl+Shift+P` and type "Reload Window" to reload VS Code.

### Step 3: Start Services

Press `F5` and select one of the available configurations:

- **Individual Services:**
  - Ciyex Backend (Auto Build) - Port 8080
  - EHR UI (Next.js) - Port 3000
  - Portal UI (Next.js) - Port 3001

- **Compound Configurations:**
  - Full Stack (Backend + EHR UI)
  - Full Stack (Backend + Portal UI)
  - **All Services (Backend + Both UIs)** ⭐ Recommended

## ✨ Features

All configurations include:

✅ **Automatic Port Checking** - Kills existing processes on ports before starting
✅ **Automatic Building** - Backend is compiled and built before launch
✅ **Dependency Installation** - npm packages are installed if needed
✅ **Browser Auto-Open** - UIs automatically open in browser when ready
✅ **Integrated Terminal** - All output visible in VS Code

## 🚀 Launch Configurations

### Backend Configuration

**Name:** Ciyex Backend (Auto Build)

**Pre-launch Steps:**
1. Check and kill any process on port 8080
2. Compile Java code with `./gradlew compileJava`
3. Build application with `./gradlew build -x test`

**Environment:**
- `SPRING_PROFILES_ACTIVE=local`

**Port:** 8080

### EHR UI Configuration

**Name:** EHR UI (Next.js)

**Pre-launch Steps:**
1. Check and kill any process on port 3000
2. Install npm dependencies if `node_modules` doesn't exist

**Command:** `npm run dev`

**Port:** 3000

**Auto-opens:** Browser at http://localhost:3000

### Portal UI Configuration

**Name:** Portal UI (Next.js)

**Pre-launch Steps:**
1. Check and kill any process on port 3001
2. Install npm dependencies if `node_modules` doesn't exist

**Command:** `npm run dev`

**Port:** 3001

**Auto-opens:** Browser at http://localhost:3001

## 🔧 Available Tasks

Access tasks via `Ctrl+Shift+P` → "Tasks: Run Task"

### Port Management
- Check and Kill Port 8080
- Check and Kill Port 3000
- Check and Kill Port 3001

### Build Tasks
- Build Backend
- Build EHR UI
- Build Portal UI

### Dependency Tasks
- Install EHR UI Dependencies
- Install Portal UI Dependencies

### Pre-launch Tasks (Auto-run)
- Pre-launch Backend
- Pre-launch EHR UI
- Pre-launch Portal UI

## 📊 Service URLs

| Service | URL | Port |
|---------|-----|------|
| Backend API | http://localhost:8080 | 8080 |
| Backend Health | http://localhost:8080/actuator/health | 8080 |
| EHR UI | http://localhost:3000 | 3000 |
| Portal UI | http://localhost:3001 | 3001 |

## 🎮 Usage Examples

### Start All Services (Recommended)

1. Press `F5`
2. Select "All Services (Backend + Both UIs)"
3. Wait for all services to start
4. Browsers will auto-open for both UIs

### Start Backend Only

1. Press `F5`
2. Select "Ciyex Backend (Auto Build)"
3. Backend will build and start on port 8080

### Start Backend + EHR UI

1. Press `F5`
2. Select "Full Stack (Backend + EHR UI)"
3. Both services start together

## 🛠️ Troubleshooting

### Port Already in Use

**Solution 1:** The pre-launch tasks automatically kill processes on ports.

**Solution 2:** Manually run the kill task:
- Press `Ctrl+Shift+P`
- Type "Tasks: Run Task"
- Select "Check and Kill Port 8080" (or 3000/3001)

### Build Fails

```bash
# Clean and rebuild
./gradlew clean build --refresh-dependencies
```

### Dependencies Not Installed

The configurations automatically install dependencies, but you can manually run:

```bash
# EHR UI
cd ciyex-ehr-ui && npm install

# Portal UI
cd ciyex-portal-ui && npm install
```

Or use the tasks:
- "Install EHR UI Dependencies"
- "Install Portal UI Dependencies"

### Configuration Not Showing

1. Verify files exist in `.vscode/` directory
2. Reload VS Code: `Ctrl+Shift+P` → "Reload Window"
3. Check JSON syntax is valid

### Backend ClassNotFoundException

The configuration uses Gradle's classpath which should fix this. If it persists:

```bash
./gradlew clean build --refresh-dependencies
```

## 📝 Configuration Files

### Location

- **Templates:** `.vscode-templates/` (version-controlled)
- **Active:** `.vscode/` (gitignored, local only)

### Files

- **`launch.json`** - Debug/launch configurations
- **`tasks.json`** - Build and port management tasks
- **`setup-vscode.sh`** - Setup script

## 🔄 Updating Configurations

If the templates are updated:

1. Run the setup script again:
   ```bash
   ./.vscode-templates/setup-vscode.sh
   ```

2. Choose to overwrite existing files when prompted

3. Reload VS Code window

## 🆚 Comparison with Shell Scripts

| Feature | VS Code Configs | Shell Scripts |
|---------|----------------|---------------|
| One-click launch | ✅ F5 | ❌ Need terminal |
| Integrated debugging | ✅ Yes | ❌ No |
| Auto port checking | ✅ Yes | ✅ Yes |
| Auto building | ✅ Yes | ✅ Yes |
| Browser auto-open | ✅ Yes | ❌ No |
| Multi-service launch | ✅ Compound configs | ✅ start-all.sh |
| IDE integration | ✅ Full | ❌ External |
| Log viewing | ✅ Integrated | ✅ Separate files |

**Recommendation:** Use VS Code configurations for development, shell scripts for CI/CD or server deployment.

## 🎓 Advanced Usage

### Debugging Backend

1. Set breakpoints in Java code
2. Press `F5` and select "Ciyex Backend (Auto Build)"
3. Backend starts in debug mode
4. Execution pauses at breakpoints

### Debugging Frontend

1. Start UI with F5
2. Open browser DevTools
3. Use browser debugger for frontend code

### Custom Environment Variables

Edit `.vscode/launch.json` backend configuration:

```json
{
  "env": {
    "SPRING_PROFILES_ACTIVE": "local",
    "CUSTOM_VAR": "value"
  }
}
```

### Changing Ports

1. Update port in service configuration (package.json for UIs)
2. Update corresponding task in `tasks.json`
3. Update launch configuration if needed

## 📚 Additional Resources

- **Full Documentation:** `RUN_CONFIGURATIONS.md`
- **Template README:** `.vscode-templates/README.md`
- **Shell Scripts:** `start-*.sh` files in project root

## 🎉 Benefits

✅ **One-click launch** - Start services with F5
✅ **No port conflicts** - Automatic port management
✅ **Always up-to-date** - Auto-build ensures latest code
✅ **Fast iteration** - Quick restart with F5
✅ **Team consistency** - Shared configurations via templates
✅ **IDE integration** - Full VS Code debugging support

---

**Last Updated**: October 23, 2025

**Questions?** Check the troubleshooting section or review the template README.
