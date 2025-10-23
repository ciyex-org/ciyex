#\!/bin/bash

# Ciyex Backend Startup Script
# Full script: compile, check port, kill if needed, and start server

set -e

PORT=8080
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "========================================="
echo "Ciyex Backend Startup"
echo "========================================="

# Compile
echo "[1/4] Compiling..."
./gradlew compileJava --no-daemon
echo "✅ Compiled"

# Check port
echo "[2/4] Checking port $PORT..."
if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null 2>&1 ; then
    PID=$(lsof -Pi :$PORT -sTCP:LISTEN -t)
    echo "⚠️  Killing process $PID on port $PORT..."
    kill -9 $PID 2>/dev/null || true
    sleep 1
fi
echo "✅ Port ready"

# Build
echo "[3/4] Building..."
./gradlew build -x test --no-daemon
echo "✅ Built"

# Start
echo "[4/4] Starting server..."
echo "========================================="
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun --no-daemon
