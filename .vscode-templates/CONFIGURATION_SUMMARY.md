# VS Code Configuration Summary

## 📦 What Was Created

### Configuration Files

1. **`.vscode-templates/launch.json`**
   - 3 individual launch configurations (Backend, EHR UI, Portal UI)
   - 3 compound configurations for running multiple services
   - Auto port checking and building
   - Browser auto-open for UIs

2. **`.vscode-templates/tasks.json`**
   - Port management tasks (kill ports 8080, 3000, 3001)
   - Build tasks (Backend, EHR UI, Portal UI)
   - Dependency installation tasks
   - Pre-launch task sequences

3. **`.vscode-templates/setup-vscode.sh`**
   - Interactive setup script
   - Copies configurations to `.vscode/`
   - Creates backups before overwriting
   - Prompts for confirmation

### Documentation

1. **`VSCODE_SETUP.md`** - Quick start guide
2. **`.vscode-templates/README.md`** - Detailed template documentation
3. **`setup-vscode-configs.sh`** - Root-level convenience script

## 🎯 Key Features

### Automatic Port Management
- Checks if ports 8080, 3000, 3001 are in use
- Automatically kills existing processes
- Prevents "port already in use" errors

### Automatic Building
- **Backend**: Compiles Java code and builds with Gradle
- **UIs**: Installs npm dependencies if needed
- Ensures you're always running the latest code

### Multi-Service Launch
- Start individual services
- Start Backend + one UI
- Start all three services together
- One-click launch with F5

### Browser Integration
- EHR UI and Portal UI auto-open in browser
- Detects when server is ready
- Opens at correct URL automatically

## 📋 Launch Configurations

### Individual Services

| Configuration | Port | Pre-launch Actions |
|--------------|------|-------------------|
| Ciyex Backend (Auto Build) | 8080 | Kill port → Compile → Build |
| EHR UI (Next.js) | 3000 | Kill port → Install deps |
| Portal UI (Next.js) | 3001 | Kill port → Install deps |

### Compound Configurations

| Configuration | Services | Ports |
|--------------|----------|-------|
| Full Stack (Backend + EHR UI) | Backend + EHR UI | 8080, 3000 |
| Full Stack (Backend + Portal UI) | Backend + Portal UI | 8080, 3001 |
| All Services (Backend + Both UIs) | All three | 8080, 3000, 3001 |

## 🛠️ Tasks Available

### Port Management
- Check and Kill Port 8080
- Check and Kill Port 3000
- Check and Kill Port 3001

### Build Tasks
- Build Backend (Gradle)
- Build EHR UI (npm)
- Build Portal UI (npm)

### Dependency Tasks
- Install EHR UI Dependencies
- Install Portal UI Dependencies

### Pre-launch Tasks (Auto-run)
- Pre-launch Backend
- Pre-launch EHR UI
- Pre-launch Portal UI

## 🚀 Installation

### Quick Install

```bash
./setup-vscode-configs.sh
```

### Manual Install

```bash
./.vscode-templates/setup-vscode.sh
```

### What It Does

1. Creates `.vscode/` directory if needed
2. Copies `launch.json` to `.vscode/`
3. Copies `tasks.json` to `.vscode/`
4. Creates backups if files exist
5. Prompts before overwriting

## 📖 Usage

### Start All Services (Recommended)

1. Press `F5`
2. Select "All Services (Backend + Both UIs)"
3. All services start with port checking and building
4. Browsers auto-open for both UIs

### Start Individual Service

1. Press `F5`
2. Select the service you want
3. Service starts with pre-launch tasks

### Run a Task Manually

1. Press `Ctrl+Shift+P`
2. Type "Tasks: Run Task"
3. Select the task

## 🔧 Technical Details

### Backend Configuration

```json
{
  "type": "java",
  "name": "Ciyex Backend (Auto Build)",
  "mainClass": "com.ciyex.CiyexApplication",
  "env": {
    "SPRING_PROFILES_ACTIVE": "local"
  },
  "preLaunchTask": "Pre-launch Backend"
}
```

**Pre-launch Task Sequence:**
1. Check and Kill Port 8080
2. Build Backend (compileJava + build -x test)

### UI Configurations

```json
{
  "type": "node-terminal",
  "name": "EHR UI (Next.js)",
  "command": "npm run dev",
  "cwd": "${workspaceFolder}/ciyex-ehr-ui",
  "preLaunchTask": "Pre-launch EHR UI",
  "serverReadyAction": {
    "pattern": "- Local:.*http://localhost:([0-9]+)",
    "action": "openExternally"
  }
}
```

**Pre-launch Task Sequence:**
1. Check and Kill Port 3000/3001
2. Install Dependencies (if node_modules missing)

### Compound Configurations

```json
{
  "name": "All Services (Backend + Both UIs)",
  "configurations": [
    "Ciyex Backend (Auto Build)",
    "EHR UI (Next.js)",
    "Portal UI (Next.js)"
  ],
  "stopAll": true
}
```

## 🎨 Customization

### Change Ports

Edit the port numbers in:
1. `tasks.json` - Port kill tasks
2. Service configuration files (package.json for UIs)

### Add Environment Variables

Edit `launch.json` backend configuration:

```json
{
  "env": {
    "SPRING_PROFILES_ACTIVE": "local",
    "YOUR_VAR": "value"
  }
}
```

### Modify Build Commands

Edit `tasks.json` build tasks:

```json
{
  "label": "Build Backend",
  "command": "./gradlew",
  "args": ["your", "custom", "args"]
}
```

## 📁 File Structure

```
ciyex/
├── .vscode/                    # Local configs (gitignored)
│   ├── launch.json            # Copied from templates
│   └── tasks.json             # Copied from templates
├── .vscode-templates/         # Version-controlled templates
│   ├── launch.json            # Launch configuration template
│   ├── tasks.json             # Tasks configuration template
│   ├── setup-vscode.sh        # Setup script
│   ├── README.md              # Template documentation
│   └── CONFIGURATION_SUMMARY.md  # This file
├── VSCODE_SETUP.md            # Quick start guide
└── setup-vscode-configs.sh    # Root-level setup script
```

## ✅ Verification

After installation, verify:

1. **Files exist:**
   ```bash
   ls -la .vscode/
   # Should show launch.json and tasks.json
   ```

2. **Configurations appear in VS Code:**
   - Press `F5`
   - Should see all 6 configurations in dropdown

3. **Tasks are available:**
   - Press `Ctrl+Shift+P`
   - Type "Tasks: Run Task"
   - Should see all tasks listed

## 🆘 Troubleshooting

### Configurations Not Showing

**Solution:**
1. Reload VS Code: `Ctrl+Shift+P` → "Reload Window"
2. Verify files in `.vscode/` directory
3. Check JSON syntax is valid

### Port Kill Not Working

**Solution:**
```bash
# Manually kill port
lsof -ti:8080 | xargs kill -9
```

### Build Fails

**Solution:**
```bash
# Clean build
./gradlew clean build --refresh-dependencies
```

### Dependencies Not Installing

**Solution:**
```bash
# Manually install
cd ciyex-ehr-ui && npm install
cd ../ciyex-portal-ui && npm install
```

## 🔄 Updating

To update configurations after template changes:

1. Run setup script again:
   ```bash
   ./setup-vscode-configs.sh
   ```

2. Choose to overwrite when prompted

3. Reload VS Code window

## 📊 Comparison with Shell Scripts

| Feature | VS Code | Shell Scripts |
|---------|---------|---------------|
| Launch method | F5 | Terminal command |
| Port checking | ✅ Auto | ✅ Auto |
| Building | ✅ Auto | ✅ Auto |
| Browser open | ✅ Auto | ❌ Manual |
| Debugging | ✅ Full support | ❌ No |
| IDE integration | ✅ Yes | ❌ No |
| Multi-service | ✅ Compounds | ✅ start-all.sh |

## 🎉 Benefits

✅ **One-click launch** - Press F5 to start
✅ **No port conflicts** - Automatic port management
✅ **Always fresh build** - Auto-compile before launch
✅ **Fast iteration** - Quick restart with F5
✅ **Team consistency** - Shared via templates
✅ **Full debugging** - VS Code debugger integration
✅ **Browser integration** - Auto-open when ready

## 📚 Related Documentation

- **Quick Start:** `VSCODE_SETUP.md`
- **Full Reference:** `RUN_CONFIGURATIONS.md`
- **Template Details:** `.vscode-templates/README.md`

---

**Created**: October 23, 2025
**Version**: 1.0.0
