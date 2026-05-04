# ============================================================
# Stage 1: Build — Maven + Java 21
# ============================================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies layer separately (faster rebuilds)
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# Build application
COPY src ./src
RUN mvn package -Dmaven.test.skip=true -B -q

# ============================================================
# Stage 2: Runtime — minimal JRE image
# ============================================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/pdf-storage

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
