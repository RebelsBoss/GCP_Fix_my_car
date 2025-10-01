# ── Build stage ────────────────────────────────────────────────────────────────
FROM eclipse-temurin:22-jdk-alpine AS builder
WORKDIR /workspace/app

# Контекст = папка backend/. Тому копіюємо без префікса backend/
COPY mvnw .
RUN sed -i 's/\r$//' mvnw && chmod 755 mvnw

COPY .mvn .mvn
COPY pom.xml .

RUN ./mvnw dependency:go-offline -B

COPY src src
RUN ./mvnw package -DskipTests

# ── Runtime stage ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:22-jdk-alpine
WORKDIR /app
COPY --from=builder /workspace/app/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
