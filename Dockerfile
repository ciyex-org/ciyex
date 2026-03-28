# ---- Build Spring Boot ----
FROM gradle:jdk24-ubi AS backend-builder

WORKDIR /app

# Copy Gradle wrapper and build files first so dependency resolution can be cached
COPY gradlew gradlew
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# Make wrapper executable
RUN chmod +x gradlew || true

# Download dependencies into the Gradle cache using BuildKit cache mount
RUN --mount=type=cache,target=/home/gradle/.gradle \
  ./gradlew --no-daemon dependencies || true

# Now copy the rest of the source and build the project (uses cached dependencies)
COPY . .
RUN --mount=type=cache,target=/home/gradle/.gradle \
  ./gradlew --no-daemon --no-build-cache clean bootJar -x test

# ---- Final Runtime ----
FROM eclipse-temurin:24-jre
WORKDIR /app

# Copy Spring Boot jar
COPY --from=backend-builder /app/build/libs/*.jar /app/app.jar

# Expose only backend port
EXPOSE 8080

# JVM flags: defaults for standalone Docker (K8s overrides via JAVA_TOOL_OPTIONS env)
# - Explicit -Xms/-Xmx instead of RAM percentages for predictable memory
# - MaxMetaspaceSize caps class metadata (unbounded by default, grows to ~250Mi)
# - G1GC: best GC for containers, UseStringDeduplication saves 5-15% String memory
# - Xss512k: reduce thread stack (default 1MB), fine with virtual threads
ENTRYPOINT ["java", \
  "-XX:+UseG1GC", \
  "-Xms128m", "-Xmx350m", \
  "-XX:+UseStringDeduplication", \
  "-XX:ActiveProcessorCount=2", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-XX:MaxMetaspaceSize=256m", \
  "-Xss512k", \
  "-jar", "/app/app.jar"]
