# ============================================================
# Multi-stage Dockerfile for AI Resume Screening System
# Stage 1: Build with Maven
# Stage 2: Run with JRE only (smaller image)
# ============================================================

# --- Build Stage ---
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy Maven wrapper and pom.xml first (layer cache)
COPY mvnw mvnw
COPY .mvn .mvn
COPY pom.xml pom.xml

# Download dependencies (cached if pom.xml unchanged)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source and build
COPY src src
RUN ./mvnw package -DskipTests -B

# --- Runtime Stage ---
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Security: run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Create uploads directory
RUN mkdir -p /app/uploads && chown appuser:appgroup /app/uploads

# Copy built jar from build stage
COPY --from=build /app/target/ai-resume-screening-1.0.0.jar app.jar

# Set ownership
RUN chown appuser:appgroup app.jar

USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

# Use exec form to allow signal handling for graceful shutdown
ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=70.0", \
  "-jar", "app.jar"]
