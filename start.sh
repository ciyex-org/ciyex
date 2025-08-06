#!/bin/bash

# Start Spring Boot in background
java -jar /app/app.jar &

# Start Next.js (SSR) in production mode
cd /app
cd ciyex-ehr-ui
npm run start &

# Wait for any process to exit (so container doesn't immediately exit)
wait -n
