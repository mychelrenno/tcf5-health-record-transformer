# Multi-stage Dockerfile for building and running the Spring Boot application
# Build stage
FROM maven:3.9.4-eclipse-temurin-21 AS builder
WORKDIR /workspace
COPY pom.xml mvnw .mvn/ ./
COPY .mvn/ .mvn/
# copy sources
COPY src ./src
# ensure mvnw is executable
RUN chmod +x mvnw || true
# Build the application (skip tests for faster builds in CI, change if you want tests run)
RUN ./mvnw -B -DskipTests package

# Run stage
FROM eclipse-temurin:21-jre
ARG JAR_FILE=target/*.jar
COPY --from=builder /workspace/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

