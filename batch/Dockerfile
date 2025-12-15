# Dockerfile
FROM eclipse-temurin:21-jdk AS app
WORKDIR /app
COPY mvnw* pom.xml ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline
COPY src ./src
CMD ["./mvnw","spring-boot:run"]
