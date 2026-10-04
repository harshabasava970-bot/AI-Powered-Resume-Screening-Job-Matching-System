# ============================================================
# Multi-stage Dockerfile for AI Resume Screening System
# Stage 1: Build with Maven wrapper (downloads deps from Maven Central)
# Stage 2: Minimal JRE runtime image
# ============================================================

# --- Build Stage ---
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy Maven wrapper first (layer cache optimization)
COPY mvnw mvnw
COPY .mvn .mvn
COPY pom.xml pom.xml

# Make wrapper executable and download all dependencies
# This layer is cached unless pom.xml changes
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B --no-transfer-progress

# Copy source code and build the fat JAR
COPY src src
RUN ./mvnw package -DskipTests -B --no-transfer-progress

# --- Runtime Stage ---
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Security: create and use a non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Create uploads directory with correct ownership
RUN mkdir -p /app/uploads && chown appuser:appgroup /app/uploads

# Copy the fat JAR from the build stage
COPY --from=build /app/target/ai-resume-screening-1.0.0.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

# Expose the port Render will map to
EXPOSE 8080

# Health check used by Render's health check probe
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# JVM flags: container-aware memory, fast startup entropy source
ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=70.0", \
  "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}", \
  "-jar", "app.jar"]
