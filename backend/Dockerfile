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

# Expose the port the application runs on
EXPOSE 8080

# Set the command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
