#\!/bin/bash

# Ciyex Portal UI Startup Script

PORT=3001
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "========================================="
echo "Ciyex Portal UI Startup"
echo "========================================="

# Check port
echo "[1/3] Checking port $PORT..."
if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null 2>&1 ; then
    PID=$(lsof -Pi :$PORT -sTCP:LISTEN -t)
    echo "⚠️  Killing process $PID on port $PORT..."
    kill -9 $PID 2>/dev/null || true
    sleep 1
fi
echo "✅ Port ready"

# Install dependencies if needed
echo "[2/3] Checking dependencies..."
cd "$PROJECT_DIR/ciyex-patient-portal"
if [ \! -d "node_modules" ]; then
    echo "   Installing dependencies..."
    npm install
fi
echo "✅ Dependencies ready"

# Start
echo "[3/3] Starting Portal UI..."
echo "========================================="
echo "Portal UI will be available at: http://localhost:$PORT"
echo "========================================="
npm run dev
