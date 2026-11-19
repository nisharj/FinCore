
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

# Copy only the executable JAR from the build stage
COPY --from=build /app/target/eureka-server-0.0.1-SNAPSHOT.jar app.jar

# Document the application port
EXPOSE 8761

# Start Eureka Server
ENTRYPOINT ["java", "-jar", "app.jar"]
