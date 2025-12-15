FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copy Maven wrapper and config
COPY mvnw* pom.xml ./
COPY .mvn .mvn

# Pre-fetch dependencies (speeds up dev container start)
RUN ./mvnw dependency:go-offline

# Source code will be mounted via volume, so we don't copy it here

# Default command runs the app in dev mode
CMD ["./mvnw", "spring-boot:run"]
