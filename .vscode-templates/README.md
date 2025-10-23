# VS Code Configuration Templates

This directory contains VS Code configuration templates for the Ciyex project.

## 📁 Files

- **`launch.json`** - Debug/launch configurations for all services
- **`tasks.json`** - Build tasks and port management tasks
- **`setup-vscode.sh`** - Setup script to copy configurations to `.vscode/`

## 🚀 Quick Setup

Run the setup script to install the configurations:

```bash
./.vscode-templates/setup-vscode.sh
```

This will:
1. Create `.vscode/` directory if it doesn't exist
2. Copy `tasks.json` and `launch.json` to `.vscode/`
3. Create backups if files already exist
4. Prompt before overwriting existing files

## 📋 Launch Configurations

### Individual Services

1. **Ciyex Backend (Auto Build)**
   - Port: 8080
   - Pre-launch: Kills port, builds with Gradle
   - Profile: `local`

2. **EHR UI (Next.js)**
   - Port: 3000
   - Pre-launch: Kills port, installs dependencies
   - Auto-opens browser when ready

3. **Portal UI (Next.js)**
   - Port: 3001
   - Pre-launch: Kills port, installs dependencies
   - Auto-opens browser when ready

### Compound Configurations

1. **Full Stack (Backend + EHR UI)**
   - Starts Backend (8080) + EHR UI (3000)

2. **Full Stack (Backend + Portal UI)**
   - Starts Backend (8080) + Portal UI (3001)

3. **All Services (Backend + Both UIs)**
   - Starts Backend (8080) + EHR UI (3000) + Portal UI (3001)

## 🛠️ Tasks

### Port Management
- **Check and Kill Port 8080** - Free backend port
- **Check and Kill Port 3000** - Free EHR UI port
- **Check and Kill Port 3001** - Free Portal UI port

### Build Tasks
- **Build Backend** - Compile and build backend with Gradle
- **Build EHR UI** - Build EHR UI production bundle
- **Build Portal UI** - Build Portal UI production bundle
- **Install EHR UI Dependencies** - Install npm packages for EHR UI
- **Install Portal UI Dependencies** - Install npm packages for Portal UI

### Pre-launch Tasks
- **Pre-launch Backend** - Kill port + Build
- **Pre-launch EHR UI** - Kill port + Install dependencies
- **Pre-launch Portal UI** - Kill port + Install dependencies

## 🎯 Usage

### Using Launch Configurations

1. Press `F5` or click Run and Debug icon
2. Select a configuration from the dropdown
3. Click the green play button or press `F5` again

### Using Tasks

1. Press `Ctrl+Shift+P`
2. Type "Tasks: Run Task"
3. Select a task from the list

## ✨ Features

✅ **Automatic port checking** - Kills existing processes before starting
✅ **Automatic building** - Compiles and builds before launching
✅ **Dependency management** - Installs npm packages if needed
✅ **Multi-service launch** - Start entire stack with one click
✅ **Browser auto-open** - Opens browser when UIs are ready
✅ **Integrated terminal** - All output in VS Code terminal

## 🔧 Customization

### Changing Ports

Edit `tasks.json` and `launch.json` to change port numbers:

**Backend (default 8080):**
- Update "Check and Kill Port 8080" task
- Update backend startup command if needed

**EHR UI (default 3000):**
- Update "Check and Kill Port 3000" task
- Update Next.js port in `ciyex-ehr-ui/package.json`

**Portal UI (default 3001):**
- Update "Check and Kill Port 3001" task
- Update Next.js port in `ciyex-portal-ui/package.json`

### Adding Environment Variables

Edit the backend configuration in `launch.json`:

```json
{
  "env": {
    "SPRING_PROFILES_ACTIVE": "local",
    "YOUR_VAR": "value"
  }
}
```

## 📝 Notes

- The `.vscode/` directory is gitignored to avoid committing local configurations
- These templates are version-controlled and can be shared with the team
- Run `setup-vscode.sh` again to update configurations after template changes
- Backups are created automatically when overwriting existing files

## 🆘 Troubleshooting

### Port Already in Use
Run the appropriate "Check and Kill Port" task manually.

### Build Fails
```bash
./gradlew clean build --refresh-dependencies
```

### Dependencies Not Installed
Run "Install EHR UI Dependencies" or "Install Portal UI Dependencies" task.

### Configuration Not Showing
1. Reload VS Code window: `Ctrl+Shift+P` → "Reload Window"
2. Check that files are in `.vscode/` directory
3. Verify JSON syntax is valid

---

**Last Updated**: October 23, 2025
