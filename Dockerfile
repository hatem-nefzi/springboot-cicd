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
COPY pom.xml .
COPY src ./src
COPY mvnw .
COPY .mvn/ .mvn/
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create config directory and copy both property files
RUN mkdir -p /config
COPY --from=builder /app/target/demo-0.0.1-SNAPSHOT.jar app.jar
COPY src/main/resources/application.properties /config/
COPY src/main/resources/application-k8s.properties /config/

# Default to port 8081, override with K8s profile
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.config.location=classpath:/,file:/config/"]