
# Stage 1: Build the Spring Boot application
FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /app

# Copy Maven configuration and source code
COPY pom.xml .
COPY src ./src

# Build the executable Spring Boot JAR
RUN mvn -B -DskipTests package

# Stage 2: Run the application
FROM eclipse-temurin:21-jre

WORKDIR /app

# Create a dedicated non-root system group and user
RUN groupadd -r fincore && useradd -r -g fincore -d /app -s /sbin/nologin fincore

# Copy the executable JAR from the build stage with non-root ownership
COPY --from=build --chown=fincore:fincore /app/target/account-service-0.0.1-SNAPSHOT.jar app.jar

# Document the application port
EXPOSE 8082

# Switch to non-root user
USER fincore

# Start Account Service
ENTRYPOINT ["java", "-jar", "app.jar"]
