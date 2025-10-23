#\!/bin/bash

# Start all Ciyex services (Backend + EHR UI + Portal UI)

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "========================================="
echo "Starting All Ciyex Services"
echo "========================================="
echo ""

# Function to kill process on port
kill_port() {
    local port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1 ; then
        PID=$(lsof -Pi :$port -sTCP:LISTEN -t)
        echo "⚠️  Killing process $PID on port $port..."
        kill -9 $PID 2>/dev/null || true
        sleep 1
    fi
}

# Kill existing processes
echo "Checking ports..."
kill_port 8080
kill_port 3000
kill_port 3001
echo "✅ All ports ready"
echo ""

# Start Backend in background
echo "Starting Backend (port 8080)..."
cd "$PROJECT_DIR"
./start-backend.sh > backend.log 2>&1 &
BACKEND_PID=$\!
echo "✅ Backend started (PID: $BACKEND_PID)"
echo ""

# Wait for backend to be ready
echo "Waiting for backend to start..."
sleep 10

# Start EHR UI in background
echo "Starting EHR UI (port 3000)..."
cd "$PROJECT_DIR/ciyex-ehr-ui"
npm run dev > ../ehr-ui.log 2>&1 &
EHR_PID=$\!
echo "✅ EHR UI started (PID: $EHR_PID)"
echo ""

# Start Portal UI in background
echo "Starting Portal UI (port 3001)..."
cd "$PROJECT_DIR/ciyex-patient-portal"
npm run dev > ../portal-ui.log 2>&1 &
PORTAL_PID=$\!
echo "✅ Portal UI started (PID: $PORTAL_PID)"
echo ""

echo "========================================="
echo "✅ All Services Started\!"
echo "========================================="
echo ""
echo "Services:"
echo "  Backend:   http://localhost:8080 (PID: $BACKEND_PID)"
echo "  EHR UI:    http://localhost:3000 (PID: $EHR_PID)"
echo "  Portal UI: http://localhost:3001 (PID: $PORTAL_PID)"
echo ""
echo "Logs:"
echo "  Backend:   tail -f backend.log"
echo "  EHR UI:    tail -f ehr-ui.log"
echo "  Portal UI: tail -f portal-ui.log"
echo ""
echo "To stop all services:"
echo "  kill $BACKEND_PID $EHR_PID $PORTAL_PID"
echo ""
echo "Press Ctrl+C to stop monitoring..."
echo "========================================="

# Keep script running and monitor processes
trap "echo 'Stopping all services...'; kill $BACKEND_PID $EHR_PID $PORTAL_PID 2>/dev/null; exit" INT TERM

# Monitor logs
tail -f backend.log ehr-ui.log portal-ui.log
