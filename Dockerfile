# ---- Build Next.js ----
FROM node:20 AS next-builder

WORKDIR /app/ciyex-ui

# Copy package files and install dependencies
COPY ciyex-ui/package*.json ./
RUN npm install

# Copy rest of the UI code
COPY ciyex-ui .

# Select which .env file to use for the build
ARG NEXT_ENV=.env
RUN if [ "$NEXT_ENV" != ".env" ] && [ -f "$NEXT_ENV" ]; then cp "$NEXT_ENV" .env; fi

# Build Next.js app (SSR)
RUN npm run build

# ---- Build Spring Boot ----
FROM gradle:8.5-jdk21 AS backend-builder

WORKDIR /app
COPY . .
RUN gradle build -x test

# ---- Final Runtime ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy Spring Boot jar
COPY --from=backend-builder /app/build/libs/*.jar /app/app.jar

# Copy built Next.js app
COPY --from=next-builder /app/ciyex-ui /app/ciyex-ui

# Install Node.js for SSR Next.js runtime
RUN apt-get update && apt-get install -y curl && \
    curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y nodejs && \
    rm -rf /var/lib/apt/lists/*

# Add startup script
COPY start.sh /app/start.sh
RUN chmod +x /app/start.sh

EXPOSE 8080 3000

ENTRYPOINT ["/app/start.sh"]
