# ---- Build EHR UI (Next.js) ----
FROM node:20 AS ehr-ui-builder

WORKDIR /app/ciyex-ehr-ui

# Install pnpm globally
RUN npm install -g pnpm

# Copy package files first to leverage Docker cache for dependencies
COPY ciyex-ehr-ui/pnpm-lock.yaml ciyex-ehr-ui/package.json ./

# Use BuildKit cache for pnpm store to speed up installs
RUN --mount=type=cache,target=/root/.pnpm-store \
    pnpm install --frozen-lockfile --store=/root/.pnpm-store

# Copy rest of the UI code
COPY ciyex-ehr-ui .

# Select environment: ENVIRONMENT=stage or ENVIRONMENT=prod
ARG ENVIRONMENT=prod

# Remove any previous .env files to avoid ambiguity
RUN rm -f .env .env.local || true

# Choose the correct env file and copy as .env (be tolerant if files are missing)
RUN if [ "$ENVIRONMENT" = "stage" ]; then \
      cp -f .env.stage .env || true; \
    elif [ "$ENVIRONMENT" = "local" ]; then \
      cp -f .env.local .env || true; \
    else \
      cp -f .env .env || true; \
    fi

# Build Next.js app (SSR)
RUN pnpm run build

# ---- Build Portal UI (Next.js) ----
FROM node:20 AS portal-ui-builder

WORKDIR /app/ciyex-portal-ui

# Copy package files first to leverage cache
COPY ciyex-portal-ui/package*.json ./

# Use BuildKit cache for npm
RUN --mount=type=cache,target=/root/.npm \
    npm install

# Copy rest of the Portal UI code
COPY ciyex-portal-ui .

# Select environment: ENVIRONMENT=stage or ENVIRONMENT=prod
ARG ENVIRONMENT=prod

# Remove any previous .env files to avoid ambiguity
RUN rm -f .env .env.local || true

# Choose the correct env file and copy as .env (be tolerant if files are missing)
RUN if [ "$ENVIRONMENT" = "stage" ]; then \
      cp -f .env.stage .env || true; \
    elif [ "$ENVIRONMENT" = "local" ]; then \
      cp -f .env.local .env || true; \
    else \
      cp -f .env .env || true; \
    fi

# Build Next.js Portal app
RUN npm run build

# ---- Build Admin UI (Next.js) ----
# FROM node:20 AS admin-ui-builder
# 
# WORKDIR /app/ciyex-admin-ui
# 
# # Copy package files and install dependencies
# COPY ciyex-admin-ui/package*.json ./
# 
# # Use BuildKit cache for npm
# RUN --mount=type=cache,target=/root/.npm \
#     npm install
# 
# # Copy rest of the Admin UI code
# COPY ciyex-admin-ui .
# 
# # Select environment: ENVIRONMENT=stage or ENVIRONMENT=prod
# ARG ENVIRONMENT=prod
# 
# # Remove any previous .env files to avoid ambiguity
# RUN rm -f .env .env.local || true
# 
# # Choose the correct env file and copy as .env (be tolerant if files are missing)
# RUN if [ "$ENVIRONMENT" = "stage" ]; then \
#       cp -f .env.stage .env || true; \
#     elif [ "$ENVIRONMENT" = "local" ]; then \
#       cp -f .env.local .env || true; \
#     else \
#       cp -f .env .env || true; \
#     fi
# 
# # Build Next.js Admin app
# RUN npm run build

# ---- Build Spring Boot ----
FROM gradle:jdk21-ubi AS backend-builder

WORKDIR /app

# Copy Gradle wrapper and build files first so dependency resolution can be cached
# (this minimizes invalidating the layer when source files change)
COPY gradlew gradlew
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# Make wrapper executable
RUN chmod +x gradlew || true

# Download dependencies into the Gradle cache using BuildKit cache mount
# Requires BuildKit: set DOCKER_BUILDKIT=1 when building locally
RUN --mount=type=cache,target=/home/gradle/.gradle \
  ./gradlew --no-daemon dependencies || true

# Now copy the rest of the source and build the project (uses cached dependencies)
COPY . .
RUN --mount=type=cache,target=/home/gradle/.gradle \
  ./gradlew --no-daemon bootJar -x test

# ---- Final Runtime ----
FROM openjdk:21-jdk-slim
WORKDIR /app

# Copy Spring Boot jar
COPY --from=backend-builder /app/build/libs/*.jar /app/app.jar

# Copy built Next.js apps
COPY --from=ehr-ui-builder /app/ciyex-ehr-ui /app/ciyex-ehr-ui
COPY --from=portal-ui-builder /app/ciyex-portal-ui /app/ciyex-portal-ui
# COPY --from=admin-ui-builder /app/ciyex-admin-ui /app/ciyex-admin-ui

# Install bash and Node.js for SSR Next.js runtime
RUN apt-get update && apt-get install -y curl bash dos2unix && \
  curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
  apt-get install -y nodejs && \
  rm -rf /var/lib/apt/lists/*

# Add startup script
COPY start.sh /app/start.sh
# Normalize line endings inside the image to avoid exec format errors (CRLF -> LF)
RUN dos2unix /app/start.sh || true
RUN chmod +x /app/start.sh

EXPOSE 8080 3000 3001 3002

ENTRYPOINT ["bash","/app/start.sh"]