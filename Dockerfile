# Stage 1: Build the application
#FROM docker.io/eclipse-temurin:17-jdk-alpine AS builder
#WORKDIR /app
#COPY . .
#RUN ./mvnw clean package -DskipTests

# Stage 2: Create a lightweight runtime image
#FROM eclipse-temurin:17-jre-alpine
#WORKDIR /app
#COPY --from=builder /app/target/demo-0.0.1-SNAPSHOT.jar app.jar
#EXPOSE 8081
#ENTRYPOINT ["java", "-jar", "app.jar"]

# Stage 1: Build the application
FROM docker.io/eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copy only the files needed for the build
COPY pom.xml .
COPY src ./src
COPY mvnw .
COPY .mvn/ .mvn/

# Make mvnw executable
RUN chmod +x mvnw

# Build the application
RUN ./mvnw clean package -DskipTests

# Stage 2: Create a lightweight runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy only the built JAR file from the builder stage
COPY --from=builder /app/target/demo-0.0.1-SNAPSHOT.jar app.jar

# Expose the application port
EXPOSE 8081

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]