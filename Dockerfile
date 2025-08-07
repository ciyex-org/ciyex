# ---- Build Next.js ----
FROM node:20 AS next-builder

WORKDIR /app/ciyex-ehr-ui

# Install pnpm globally
RUN npm install -g pnpm

# Copy package files and install dependencies
COPY ciyex-ehr-ui/pnpm-lock.yaml ciyex-ehr-ui/package.json ./
RUN pnpm install --frozen-lockfile

# Copy rest of the UI code
COPY ciyex-ehr-ui .

# Select environment: ENVIRONMENT=stage or ENVIRONMENT=prod
ARG ENVIRONMENT=prod

# Remove any previous .env files to avoid ambiguity
RUN rm -f .env .env.local

# Choose the correct env file and copy as .env
RUN if [ "$ENVIRONMENT" = "stage" ]; then \
      cp .env.stage .env; \
    elif [ "$ENVIRONMENT" = "local" ]; then \
      cp .env.local .env; \
    else \
      cp .env .env; \
    fi

# Build Next.js app (SSR)
RUN pnpm run build

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
COPY --from=next-builder /app/ciyex-ehr-ui /app/ciyex-ehr-ui

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
