# ====================================================================================
# STAGE 1: Build the application using Maven
# ====================================================================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

# Set the working directory
WORKDIR /app

# Copy the Maven project file and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the rest of the source code and build the application JAR
COPY src ./src
RUN mvn package -DskipTests

# ====================================================================================
# STAGE 2: Create the final, lightweight runtime image
# ====================================================================================
FROM eclipse-temurin:21-jre-alpine

# Set the working directory
WORKDIR /app

# Copy the executable JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Cloud Run injects a dynamic port via the PORT environment variable (usually 8080).
# Exposing a static port in Docker is ignored by Cloud Run, but we define the default here.
ENV PORT=8080
EXPOSE 8080

# OPTIMIZED RUN COMMAND FOR CLOUD RUN:
# 1. We dynamically bind to the $PORT variable provided by Google Cloud.
# 2. We add JVM optimizations (-XX:+UseG1GC, -XX:MaxRAMPercentage) to keep memory usage low.
ENTRYPOINT ["java", "-Dserver.port=${PORT}", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
