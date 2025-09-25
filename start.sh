

#!/bin/bash

# Start Spring Boot in background
java -jar /app/app.jar &

# Start EHR UI (Next.js) on port 3000
cd /app/ciyex-ehr-ui
npm run start &

# Start Portal UI (Next.js) on port 3001
cd /app/ciyex-portal-ui
PORT=3001 npm run start &

# Start Admin UI (Next.js) on port 3002
cd /app/ciyex-admin-ui
PORT=3002 npm run start &

# Wait for any process to exit (so container doesn't immediately exit)
wait -n

