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
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Copy Spring Boot jar
COPY --from=backend-builder /app/build/libs/*.jar /app/app.jar

# Install bash for any startup scripts
RUN apt-get update && apt-get install -y bash && \
  rm -rf /var/lib/apt/lists/*

# Expose only backend port
EXPOSE 8080

# Run Spring Boot application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
