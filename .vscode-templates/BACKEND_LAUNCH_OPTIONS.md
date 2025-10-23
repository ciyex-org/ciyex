# Backend Launch Configuration Options

## Overview

There are two backend launch configurations available, each optimized for different use cases.

## 🚀 Ciyex Backend (Auto Build)

**Type:** Terminal-based launch using Gradle bootRun

**Best for:**
- Quick development and testing
- Running the application without debugging
- Avoiding classpath issues
- Faster startup

**How it works:**
```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun --no-daemon
```

**Pre-launch steps:**
1. Kills any process on port 8080
2. Compiles Java code (`./gradlew compileJava`)
3. Builds application (`./gradlew build -x test`)
4. Runs application with Gradle bootRun

**Pros:**
✅ No classpath issues - Gradle handles everything
✅ Reliable and consistent
✅ Same as running from command line
✅ Works out of the box

**Cons:**
❌ No breakpoint debugging
❌ No step-through debugging

## 🐛 Ciyex Backend (Debug Mode)

**Type:** Java debugger launch

**Best for:**
- Debugging with breakpoints
- Step-through code execution
- Inspecting variables
- Advanced debugging features

**How it works:**
- Launches `com.qiaben.ciyex.CiyexApplication` directly
- Attaches Java debugger
- Full IDE debugging support

**Pre-launch steps:**
1. Kills any process on port 8080
2. Compiles Java code
3. Builds application
4. Launches with Java debugger attached

**Pros:**
✅ Full debugging support
✅ Set breakpoints in code
✅ Step through execution
✅ Inspect variables
✅ VS Code debugging UI

**Cons:**
❌ May have classpath issues if dependencies change
❌ Requires proper Java extension setup
❌ Slightly more complex setup

## 📊 Comparison

| Feature | Auto Build | Debug Mode |
|---------|-----------|------------|
| Launch method | Gradle bootRun | Java debugger |
| Debugging | ❌ No | ✅ Yes |
| Breakpoints | ❌ No | ✅ Yes |
| Classpath handling | ✅ Automatic | ⚠️ Manual |
| Startup speed | ✅ Fast | ⚠️ Slower |
| Reliability | ✅ High | ⚠️ Medium |
| Use case | Development | Debugging |

## 🎯 When to Use Each

### Use "Auto Build" when:
- You're doing rapid development
- You don't need to debug
- You want reliable startup
- You're testing features
- You're running integration tests

### Use "Debug Mode" when:
- You need to set breakpoints
- You're investigating a bug
- You need to inspect variables
- You want to step through code
- You're doing deep debugging

## 🔧 Troubleshooting

### Auto Build Issues

**Problem:** Build fails
```bash
# Solution: Clean build
./gradlew clean build --refresh-dependencies
```

**Problem:** Port already in use
- The pre-launch task automatically kills port 8080
- If it persists, manually run: `lsof -ti:8080 | xargs kill -9`

### Debug Mode Issues

**Problem:** ClassNotFoundException
- This happens when classpath isn't properly configured
- **Solution 1:** Use "Auto Build" configuration instead
- **Solution 2:** Rebuild project: `./gradlew clean build`
- **Solution 3:** Refresh dependencies: `./gradlew --refresh-dependencies`

**Problem:** Debugger won't attach
- Ensure Java Extension Pack is installed in VS Code
- Reload VS Code window
- Try "Auto Build" configuration first to verify app works

**Problem:** Breakpoints not hitting
- Ensure code is compiled: `./gradlew compileJava`
- Check breakpoint is on executable line
- Verify correct source file is open

## 💡 Recommendations

### For Daily Development
Use **"Ciyex Backend (Auto Build)"** as your default. It's reliable, fast, and handles classpath automatically.

### For Debugging Sessions
Switch to **"Ciyex Backend (Debug Mode)"** when you need to investigate issues with breakpoints.

### For Compound Configurations
The compound configurations (Full Stack, All Services) use "Auto Build" by default for reliability.

## 🔄 Switching Between Modes

You can easily switch between modes:

1. Stop current configuration (red square button)
2. Press F5
3. Select the other backend configuration
4. Start

## 📝 Configuration Details

### Auto Build Configuration
```json
{
  "type": "node-terminal",
  "name": "Ciyex Backend (Auto Build)",
  "request": "launch",
  "command": "SPRING_PROFILES_ACTIVE=local ./gradlew bootRun --no-daemon",
  "cwd": "${workspaceFolder}",
  "preLaunchTask": "Pre-launch Backend"
}
```

### Debug Mode Configuration
```json
{
  "type": "java",
  "name": "Ciyex Backend (Debug Mode)",
  "request": "launch",
  "mainClass": "com.qiaben.ciyex.CiyexApplication",
  "projectName": "ciyex",
  "env": {
    "SPRING_PROFILES_ACTIVE": "local"
  },
  "preLaunchTask": "Pre-launch Backend"
}
```

## 🎓 Advanced Tips

### Remote Debugging
If you need remote debugging capabilities, you can modify the Auto Build configuration:

```json
{
  "command": "SPRING_PROFILES_ACTIVE=local ./gradlew bootRun --no-daemon --debug-jvm"
}
```

Then attach a remote debugger on port 5005.

### Custom JVM Args
For Debug Mode, add custom JVM arguments:

```json
{
  "vmArgs": "-Dspring.profiles.active=local -Xmx2g -Xms512m"
}
```

### Environment Variables
Both configurations support environment variables:

```json
{
  "env": {
    "SPRING_PROFILES_ACTIVE": "local",
    "CUSTOM_VAR": "value"
  }
}
```

---

**Last Updated**: October 23, 2025

**Recommendation**: Start with "Auto Build" for development, use "Debug Mode" when needed.
